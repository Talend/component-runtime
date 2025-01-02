/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.components.vault.client;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.annotation.JsonbProperty;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.components.vault.configuration.Documentation;
import org.talend.sdk.components.vault.server.error.ErrorPayload;
import org.talend.sdk.components.vault.server.error.ErrorPayload.ErrorDictionary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@ApplicationScoped
public class VaultClient {

    @Inject
    private VaultClientSetup setup;

    @Inject
    @VaultHttp
    private WebTarget vault;

    @Inject
    @Documentation("The vault path to retrieve a token.")
    @ConfigProperty(name = "talend.vault.cache.vault.auth.endpoint", defaultValue = "v1/auth/engines/login")
    private String authEndpoint;

    @Inject
    @Documentation("The vault path to decrypt values. You can use the variable `{x-talend-tenant-id}` to replace by `x-talend-tenant-id` header value.")
    @ConfigProperty(name = "talend.vault.cache.vault.decrypt.endpoint",
            defaultValue = "v1/tenants-keyrings/decrypt/{x-talend-tenant-id}")
    private String decryptEndpoint;

    @Inject
    @Documentation("The vault token to use to log in (will make roleId and secretId ignored). `-` means it is ignored.")
    @ConfigProperty(name = "talend.vault.cache.vault.auth.token", defaultValue = "-")
    private Supplier<String> token;

    @Inject
    @Documentation("The vault role identifier to use to log in (if token is not set). `-` means it is ignored.")
    @ConfigProperty(name = "talend.vault.cache.vault.auth.roleId", defaultValue = "-")
    private Supplier<String> role;

    @Inject
    @Documentation("The vault secret identifier to use to log in (if token is not set). `-` means it is ignored.")
    @ConfigProperty(name = "talend.vault.cache.vault.auth.secretId", defaultValue = "-")
    private Supplier<String> secret;

    @Inject
    @Documentation("How often (in ms) to refresh the vault token.")
    @ConfigProperty(name = "talend.vault.cache.service.auth.refreshDelayMargin", defaultValue = "600000")
    private Long refreshDelayMargin;

    @Inject
    @Documentation("How long (in ms) to wait before retrying a recoverable error or refresh the vault token in case of an authentication failure.")
    @ConfigProperty(name = "talend.vault.cache.service.auth.refreshDelayOnFailure", defaultValue = "1000")
    private Long refreshDelayOnFailure;

    @Inject
    @Documentation("How many times do we retry a recoverable operation in case of a failure.")
    @ConfigProperty(name = "talend.vault.cache.service.auth.numberOfRetryOnFailure", defaultValue = "3")
    private Integer numberOfRetryOnFailure;

    @Inject
    @Documentation("Status code sent when vault can't decipher some values.")
    @ConfigProperty(name = "talend.vault.cache.service.auth.cantDecipherStatusCode", defaultValue = "422")
    private Integer cantDecipherStatusCode;

    @Inject
    @Documentation("The regex to whitelist ciphered keys, others will be passthrough in the output without going to vault.")
    @ConfigProperty(name = "talend.vault.cache.service.decipher.skip.regex", defaultValue = "vault\\:v[0-9]+\\:.*")
    private String passthroughRegex;

    @Inject
    private Cache<String, DecryptedValue> cache;

    @Inject
    private Clock clock;

    private final AtomicReference<Authentication> authToken = new AtomicReference<>();

    private ScheduledExecutorService scheduledExecutorService;

    private Pattern compiledPassthroughRegex;

    private final Predicate<Throwable> shouldRetry = cause -> {
        if (WebApplicationException.class.isInstance(cause)) {
            final WebApplicationException wae = WebApplicationException.class.cast(cause);
            final int status = wae.getResponse().getStatus();
            if (Status.NOT_FOUND.getStatusCode() == status || status >= 500) {
                return false;
            }
        }
        return true;
    };

    @PostConstruct
    private void init() {
        compiledPassthroughRegex = Pattern.compile(passthroughRegex);
    }

    @PreDestroy
    private void destroy() {
        scheduledExecutorService.shutdownNow(); // we don't care anymore about these tasks
        try {
            scheduledExecutorService.awaitTermination(1L, MINUTES); // wait too much but enough for our goal
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // as deprecation was introduced since = "17", can ignore it for now...
    @SuppressWarnings({ "deprecation", "removal" })
    public void init(@Observes @Initialized(ApplicationScoped.class) final ServletContext init) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            private final ThreadGroup group = ofNullable(System.getSecurityManager())
                    .map(SecurityManager::getThreadGroup)
                    .orElseGet(() -> Thread.currentThread().getThreadGroup());

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(group, r, "talend-vault-service-refresh", 0L);
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        });
    }

    @SneakyThrows
    public Map<String, String> decrypt(final Map<String, String> values) {
        return decrypt(values, null);
    }

    @SneakyThrows
    public Map<String, String> decrypt(final Map<String, String> values, final String tenantId) {
        if ("no-vault".equals(setup.getVaultUrl())) {
            return values;
        }
        final List<String> cipheredKeys = values
                .entrySet()
                .stream()
                .filter(entry -> compiledPassthroughRegex.matcher(entry.getValue()).matches())
                .map(cyphered -> cyphered.getKey())
                .collect(toList());
        if (cipheredKeys.isEmpty()) {
            return values;
        }
        Supplier<CompletableFuture<Map<String, String>>> attempt = () -> prepareRequest(values, cipheredKeys, tenantId);
        return withRetries(attempt, shouldRetry).get();
    }

    private CompletableFuture<Map<String, String>> prepareRequest(final Map<String, String> values,
            final List<String> cipheredKeys, final String tenantId) {
        return get(cipheredKeys.stream().map(values::get).collect(toList()), clock.millis(), tenantId)
                .thenApply(decrypted -> values
                        .entrySet()
                        .stream()
                        .collect(toMap(Entry::getKey,
                                e -> of(cipheredKeys.indexOf(e.getKey()))
                                        .filter(idx -> idx >= 0)
                                        .map(decrypted::get)
                                        .map(DecryptedValue::getValue)
                                        .orElseGet(() -> values.get(e.getKey())))));
    }

    private CompletableFuture<List<DecryptedValue>> get(final Collection<String> values, final long currentTime,
            final String tenantId) {
        final AtomicInteger index = new AtomicInteger();
        final Collection<EntryWithIndex<String>> clearValues = values
                .stream()
                .map(it -> new EntryWithIndex<>(index.getAndIncrement(), it))
                .filter(it -> it.entry != null && !compiledPassthroughRegex.matcher(it.entry).matches())
                .collect(toList());
        if (clearValues.isEmpty()) {
            return doDecipher(values, currentTime, tenantId).toCompletableFuture();
        }
        if (clearValues.size() == values.size()) {
            final long now = clock.millis();
            return completedFuture(values.stream().map(it -> new DecryptedValue(it, now)).collect(toList()));
        }
        return doDecipher(values, currentTime, tenantId).thenApply(deciphered -> {
            final long now = clock.millis();
            clearValues.forEach(entry -> deciphered.add(entry.index, new DecryptedValue(entry.entry, now)));
            return deciphered;
        }).toCompletableFuture();
    }

    private CompletionStage<List<DecryptedValue>> doDecipher(final Collection<String> values, final long currentTime,
            final String tenantId) {
        final Map<String, Optional<DecryptedValue>> alreadyCached =
                new HashSet<>(values).stream().collect(toMap(identity(), it -> ofNullable(cache.get(it))));
        final Collection<String> missing = alreadyCached
                .entrySet()
                .stream()
                .filter(it -> !it.getValue().isPresent())
                .map(Map.Entry::getKey)
                .collect(toList());
        if (missing.isEmpty()) { // no remote call, yeah
            return completedFuture(values.stream().map(alreadyCached::get).map(Optional::get).collect(toList()));
        }
        // do request
        return getOrRequestAuth()
                // prepare decrypt request to vault
                .thenCompose(auth -> ofNullable(auth.getAuth()).map(Auth::getClientToken).map(clientToken -> {
                    WebTarget path = vault.path(decryptEndpoint);
                    if (decryptEndpoint.contains("x-talend-tenant-id")) {
                        path = path
                                .resolveTemplate("x-talend-tenant-id",
                                        ofNullable(tenantId)
                                                .orElseThrow(() -> new WebApplicationException(Response
                                                        .status(Status.NOT_FOUND)
                                                        .entity(new ErrorPayload(ErrorDictionary.BAD_FORMAT,
                                                                "No header x-talend-tenant-id"))
                                                        .build())));
                    }
                    return path
                            .request(APPLICATION_JSON_TYPE)
                            .header("X-Vault-Token", clientToken)
                            .rx()
                            .post(entity(new DecryptRequest(
                                    missing.stream().map(it -> new DecryptInput(it, null, null)).collect(toList())),
                                    APPLICATION_JSON_TYPE), DecryptResponse.class)
                            .toCompletableFuture()
                            // fetch decrypted values
                            .thenApply(decrypted -> {
                                final Collection<DecryptResult> results = decrypted.getData().getBatchResults();
                                if (results.isEmpty()) {
                                    throwError(cantDecipherStatusCode, "Decrypted values are empty");
                                }
                                final List<String> errors = results
                                        .stream()
                                        .map(DecryptResult::getError)
                                        .filter(Objects::nonNull)
                                        .collect(toList());
                                if (!errors.isEmpty()) {
                                    throwError(cantDecipherStatusCode, "Can't decipher properties: " + errors);
                                }
                                final Iterator<String> keyIterator = missing.iterator();
                                final Map<String, DecryptedValue> decryptedResults = results
                                        .stream()
                                        .map(it -> new String(Base64.getDecoder().decode(it.getPlaintext()),
                                                StandardCharsets.UTF_8))
                                        .collect(toMap(it -> keyIterator.next(),
                                                it -> new DecryptedValue(it, currentTime)));
                                cache.putAll(decryptedResults);
                                //
                                return values
                                        .stream()
                                        .map(it -> decryptedResults
                                                .getOrDefault(it, alreadyCached.get(it).orElse(null)))
                                        .collect(toList());
                            })
                            // oops, smtg went wrong
                            .exceptionally(e -> {
                                final Throwable cause = e.getCause();
                                String message = "";
                                int status = cantDecipherStatusCode;
                                if (WebApplicationException.class.isInstance(cause)) {
                                    final WebApplicationException wae = WebApplicationException.class.cast(cause);
                                    final Response response = wae.getResponse();
                                    if (response != null) {
                                        if (ErrorPayload.class.isInstance(response.getEntity())) { // internal error
                                            throw wae;
                                        } else {
                                            try {
                                                message = response.readEntity(String.class);
                                            } catch (final Exception ignored) {
                                                // no-op
                                            }
                                        }
                                        status = response.getStatus();
                                        if (status == Status.NOT_FOUND.getStatusCode() && message.isEmpty()) {
                                            message = "Decryption failed: Endpoint not found, check your setup.";
                                        }
                                    }
                                }
                                if (message.isEmpty()) {
                                    message = String.format("Decryption failed: %s", cause.getMessage());
                                }
                                log.error("{} ({}).", message, status);
                                throw new WebApplicationException(message,
                                        Response
                                                .status(status)
                                                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, message))
                                                .build());
                            });
                })
                        .orElseThrow(() -> new WebApplicationException(Response
                                .status(Response.Status.FORBIDDEN)
                                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, "getOrRequestAuth failed"))
                                .build())));
    }

    private CompletionStage<Authentication> getOrRequestAuth() {
        return of(token.get()).filter(this::isReloadableConfigSet).map(value -> {
            final Auth authInfo = new Auth();
            authInfo.setClientToken(value);
            authInfo.setLeaseDuration(Long.MAX_VALUE);
            authInfo.setRenewable(false);
            return completedFuture(new Authentication(authInfo, Long.MAX_VALUE));
        }).orElseGet(() -> {
            final String role = of(this.role.get()).filter(this::isReloadableConfigSet).orElse(null);
            final String secret = of(this.secret.get()).filter(this::isReloadableConfigSet).orElse(null);
            return ofNullable(authToken.get())
                    .filter(auth -> (auth.getExpiresAt() - clock.millis()) <= refreshDelayMargin) // is expired
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> doAuth(role, secret).toCompletableFuture());
        });
    }

    private CompletionStage<Authentication> doAuth(final String role, final String secret) {
        log.info("Authenticating to vault");
        return vault
                .path(authEndpoint)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .post(entity(new AuthRequest(role, secret), APPLICATION_JSON_TYPE), AuthResponse.class)
                //
                .thenApply(token -> {
                    log.debug("Authenticated to vault");
                    if (token.getAuth() == null || token.getAuth().getClientToken() == null) {
                        throwError(500, "Vault didn't return a token");
                    } else {
                        log.info("Authenticated to vault");
                    }
                    final long validityMargin = TimeUnit.SECONDS.toMillis(token.getAuth().getLeaseDuration());
                    final long nextRefresh = clock.millis() + validityMargin - refreshDelayMargin;
                    final Authentication authentication = new Authentication(token.getAuth(), nextRefresh);
                    authToken.set(authentication);
                    if (!scheduledExecutorService.isShutdown() && token.getAuth().isRenewable()) {
                        scheduledExecutorService.schedule(() -> doAuth(role, secret), nextRefresh, MILLISECONDS);
                    }
                    return authentication;
                })
                //
                .exceptionally(e -> {
                    final Throwable cause = e.getCause();
                    if (WebApplicationException.class.isInstance(cause)) {
                        final WebApplicationException wae = WebApplicationException.class.cast(cause);
                        final Response response = wae.getResponse();
                        String message = "";
                        if (ErrorPayload.class.isInstance(wae.getResponse().getEntity())) {
                            throw wae; // already logged and setup broken so just rethrow
                        } else {
                            try {
                                message = response.readEntity(String.class);
                            } catch (final Exception ignored) {
                                // no-op
                            }
                            if (message.isEmpty()) {
                                message = cause.getMessage();
                            }
                            throwError(response.getStatus(), message);
                        }
                    }
                    throwError(cause);
                    return null;
                });
    }

    private <T> CompletableFuture<T> withRetries(final Supplier<CompletableFuture<T>> attempt,
            final Predicate<Throwable> shouldRetry) {
        Executor scheduler = r -> scheduledExecutorService.schedule(r, refreshDelayOnFailure, TimeUnit.MILLISECONDS);
        CompletableFuture<T> firstAttempt = attempt.get();
        return flatten(firstAttempt
                .thenApply(CompletableFuture::completedFuture)
                .exceptionally(throwable -> retryFuture(attempt, 1, throwable, shouldRetry, scheduler)));
    }

    private <T> CompletableFuture<T> retryFuture(final Supplier<CompletableFuture<T>> attempter,
            final int attemptsSoFar, final Throwable throwable, final Predicate<Throwable> shouldRetry,
            final Executor scheduler) {
        int nextAttempt = attemptsSoFar + 1;
        log
                .info("[retryFuture] Retry failed operation ({}/{}). Reason: {}.", attemptsSoFar,
                        numberOfRetryOnFailure, throwable.getMessage());
        if (nextAttempt > numberOfRetryOnFailure || !shouldRetry.test(throwable.getCause())) {
            log.info("[retryFuture] Stop retry failed operation (condition triggered).");
            throwError(throwable.getCause());
        }
        return flatten(flatten(CompletableFuture.supplyAsync(attempter, scheduler))
                .thenApply(CompletableFuture::completedFuture)
                .exceptionally(
                        nextThrowable -> retryFuture(attempter, nextAttempt, nextThrowable, shouldRetry, scheduler)));
    }

    private <T> CompletableFuture<T> flatten(final CompletableFuture<CompletableFuture<T>> completableCompletable) {
        return completableCompletable.thenCompose(Function.identity());
    }

    private void throwError(final int status, final String message) {
        throw new WebApplicationException(message,
                Response.status(status).entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, message)).build());
    }

    private void throwError(final Throwable cause) {
        String message = "";
        int status = cantDecipherStatusCode;
        if (WebApplicationException.class.isInstance(cause)) {
            final WebApplicationException wae = WebApplicationException.class.cast(cause);
            final Response response = wae.getResponse();
            status = response.getStatus();
            if (response != null) {
                if (ErrorPayload.class.isInstance(response.getEntity())) { // internal error
                    throw wae;
                } else {
                    try {
                        message = response.readEntity(String.class);
                    } catch (final Exception ignored) {
                        // no-op
                    }
                }
            }
        }
        if (message.isEmpty()) {
            message = cause.getMessage();
        }
        throw new WebApplicationException(message,
                Response.status(status).entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, message)).build());
    }

    // workaround while geronimo-config does not support generics of generics
    // (1.2.1 in org.apache.geronimo.config.cdi.ConfigInjectionBean.create)
    private boolean isReloadableConfigSet(final String value) {
        return !"-".equals(value);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecryptInput {

        private String ciphertext;

        private String context; // only when derivation is activated

        private String nonce; // only when convergent encryption is activated
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecryptRequest {

        @JsonbProperty("batch_input")
        private Collection<DecryptInput> batchInput;
    }

    @Data
    public static class DecryptResult {

        private String plaintext;

        private String context;

        private String error;
    }

    @Data
    public static class DecryptResponse {

        private DecryptData data;
    }

    @Data
    public static class DecryptData {

        @JsonbProperty("batch_results")
        private Collection<DecryptResult> batchResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthRequest {

        @JsonbProperty("role_id")
        private String roleId;

        @JsonbProperty("secret_id")
        private String secretId;
    }

    @Data
    public static class Auth {

        private boolean renewable;

        @JsonbProperty("lease_duration")
        private long leaseDuration;

        @JsonbProperty("client_token")
        private String clientToken;
    }

    @Data
    public static class AuthResponse {

        private Auth auth;
    }

    @Data
    private static class Authentication {

        private final Auth auth;

        private final long expiresAt;
    }

    @RequiredArgsConstructor
    private static class EntryWithIndex<T> {

        private final int index;

        private final T entry;
    }
}
