/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.server.vault.proxy.service;

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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.runtime.server.vault.proxy.configuration.Documentation;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// todo: the suppliers should probably be replaced by invalidating a config bean
// and become plain String or Optional<String>
@Slf4j
@ApplicationScoped
public class VaultService {

    @Inject
    @Http(Http.Type.VAULT)
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
    @Documentation("How often (in ms) to refresh the vault token in case of an authentication failure.")
    @ConfigProperty(name = "talend.vault.cache.service.auth.refreshDelayOnFailure", defaultValue = "10000")
    private Long refreshDelayOnFailure;

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

    @PostConstruct
    private void init() {
        compiledPassthroughRegex = Pattern.compile(passthroughRegex);
    }

    public CompletableFuture<List<DecryptedValue>> get(final Collection<String> values, final long currentTime,
            final HttpHeaders headers) {
        final AtomicInteger index = new AtomicInteger();
        final Collection<EntryWithIndex<String>> clearValues = values
                .stream()
                .map(it -> new EntryWithIndex<>(index.getAndIncrement(), it))
                .filter(it -> it.entry != null && !compiledPassthroughRegex.matcher(it.entry).matches())
                .collect(toList());
        if (clearValues.isEmpty()) {
            return doDecipher(values, currentTime, headers).toCompletableFuture();
        }
        if (clearValues.size() == values.size()) {
            final long now = clock.millis();
            return completedFuture(values.stream().map(it -> new DecryptedValue(it, now)).collect(toList()));
        }
        return doDecipher(values, currentTime, headers).thenApply(deciphered -> {
            final long now = clock.millis();
            clearValues.forEach(entry -> deciphered.add(entry.index, new DecryptedValue(entry.entry, now)));
            return deciphered;
        }).toCompletableFuture();
    }

    private CompletionStage<List<DecryptedValue>> doDecipher(final Collection<String> values, final long currentTime,
            final HttpHeaders headers) {
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
        return getOrRequestAuth()
                .thenCompose(auth -> ofNullable(auth.getAuth()).map(Auth::getClientToken).map(clientToken -> {
                    WebTarget path = vault.path(decryptEndpoint);
                    if (decryptEndpoint.contains("x-talend-tenant-id")) {
                        path = path
                                .resolveTemplate("x-talend-tenant-id",
                                        ofNullable(headers.getHeaderString("x-talend-tenant-id"))
                                                .orElseThrow(() -> new WebApplicationException(Response
                                                        .status(Response.Status.BAD_REQUEST)
                                                        .entity("{\"message\":\"No header x-talend-tenant-id\"}")
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
                            .thenApply(decrypted -> {
                                final Collection<DecryptResult> results = decrypted.getData().getBatchResults();
                                if (results.isEmpty()) {
                                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                                }
                                final List<String> errors = results
                                        .stream()
                                        .map(DecryptResult::getError)
                                        .filter(Objects::nonNull)
                                        .collect(toList());
                                if (!errors.isEmpty()) {
                                    throw new WebApplicationException(Response
                                            .status(cantDecipherStatusCode)
                                            .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED,
                                                    "Can't decipher properties: " + errors))
                                            .build());
                                }

                                final Iterator<String> keyIterator = missing.iterator();
                                final Map<String, DecryptedValue> decryptedResults = results
                                        .stream()
                                        .map(it -> new String(Base64.getDecoder().decode(it.getPlaintext()),
                                                StandardCharsets.UTF_8))
                                        .collect(toMap(it -> keyIterator.next(),
                                                it -> new DecryptedValue(it, currentTime)));

                                cache.putAll(decryptedResults);

                                return values
                                        .stream()
                                        .map(it -> decryptedResults
                                                .getOrDefault(it, alreadyCached.get(it).orElse(null)))
                                        .collect(toList());
                            })
                            .exceptionally(e -> { // we don't cache failure for now since it is not supposed to
                                                  // happen
                                final Throwable cause = e.getCause();
                                String debug = "";
                                if (WebApplicationException.class.isInstance(cause)) {
                                    final WebApplicationException wae = WebApplicationException.class.cast(cause);
                                    final Response response = wae.getResponse();
                                    if (response != null) {
                                        if (ErrorPayload.class.isInstance(response.getEntity())) { // internal error
                                            log
                                                    .error("{}",
                                                            ErrorPayload.class
                                                                    .cast(response.getEntity())
                                                                    .getDescription());
                                            throw wae;
                                        } else {
                                            try {
                                                debug = response.readEntity(String.class);
                                            } catch (final Exception ignored) {
                                                // no-op
                                            }
                                        }

                                        final int status = response.getStatus();
                                        if (status == Response.Status.NOT_FOUND.getStatusCode()) {
                                            log
                                                    .error("Failed to decrypt to vault, endpoint not found, check your setup",
                                                            e);
                                            return null;
                                        }
                                    }
                                }
                                log.error("Failed to decrypt, debug='" + debug + "'", e);
                                throw new WebApplicationException(cantDecipherStatusCode);
                            });
                }).orElseThrow(() -> new WebApplicationException(Response.Status.FORBIDDEN)));
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) final ServletContext init) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            private final ThreadGroup group = ofNullable(System.getSecurityManager())
                    .map(SecurityManager::getThreadGroup)
                    .orElseGet(() -> Thread.currentThread().getThreadGroup());

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(group, r, "talend-vault-service-refresh", 0);
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        });
        // note: by default we start without the token so no: scheduledExecutorService.submit(this::getOrRequestAuth);
    }

    @PreDestroy
    private void destroy() {
        scheduledExecutorService.shutdownNow(); // we don't care anymore about these tasks
        try {
            scheduledExecutorService.awaitTermination(1, MINUTES); // wait too much but enough for our goal
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            if (role == null) {
                log.error("No vault token or role available, authentication will not be possible");
                throw new WebApplicationException(Response
                        .serverError()
                        .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, "Vault not reachable"))
                        .build());
            }
            return ofNullable(authToken.get())
                    .filter(auth -> (auth.getExpiresAt() - clock.millis()) <= refreshDelayMargin) // is expired
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> doAuth(role).toCompletableFuture());
        });
    }

    private CompletionStage<Authentication> doAuth(final String role) {
        log.info("Authenticating to vault");
        return vault
                .path(authEndpoint)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .post(entity(new AuthRequest(role, of(secret.get()).filter(this::isReloadableConfigSet).orElse(null)),
                        APPLICATION_JSON_TYPE), AuthResponse.class)
                .thenApply(token -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Authenticated to vault '{}'", token);
                    }

                    if (token.getAuth() == null || token.getAuth().getClientToken() == null) {
                        log.error("Vault didn't return a token");
                        throw new WebApplicationException(Response
                                .serverError()
                                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, "Vault not available"))
                                .build());
                    } else {
                        log.info("Authenticated to vault");
                    }

                    final long validityMargin = TimeUnit.SECONDS.toMillis(token.getAuth().getLeaseDuration());
                    final long nextRefresh = clock.millis() + validityMargin - refreshDelayMargin;
                    final Authentication authentication = new Authentication(token.getAuth(), nextRefresh);
                    authToken.set(authentication);
                    if (!scheduledExecutorService.isShutdown() && token.getAuth().isRenewable()) {
                        scheduledExecutorService.schedule(() -> doAuth(role), nextRefresh, MILLISECONDS);
                    }
                    return authentication;
                })
                .exceptionally(e -> {
                    final Throwable cause = e.getCause();
                    String debug = "";
                    if (WebApplicationException.class.isInstance(cause)) {
                        final WebApplicationException wae = WebApplicationException.class.cast(cause);
                        final Response response = wae.getResponse();
                        if (response != null) {
                            if (ErrorPayload.class.isInstance(wae.getResponse().getEntity())) {
                                // already logged and setup broken so just rethrow
                                throw wae;
                            } else {
                                try {
                                    debug = response.readEntity(String.class);
                                } catch (final Exception ignored) {
                                    // no-op
                                }

                                final int status = response.getStatus();
                                if (status == Response.Status.NOT_FOUND.getStatusCode()) {
                                    log
                                            .error("Failed to authenticate to vault, endpoint not found, check your setup",
                                                    e);
                                    return null;
                                }
                                if (status == Response.Status.FORBIDDEN.getStatusCode()) {
                                    log
                                            .error("Failed to authenticate to vault, forbidden access, check your setup (key)",
                                                    e);
                                    return null;
                                }
                                if (status == 429) { // rate limit reached, wait
                                    log.error("Failed to authenticate to vault, rate limit reached", e);
                                    return null;
                                }
                                if (status >= 500) { // something wrong, no need to retry
                                    log.error("Failed to authenticate to vault, unexpected error", e);
                                    return null;
                                }
                            }
                        }
                    }
                    log.error("Failed to authenticate to vault, retrying, debug='" + debug + "'", e);
                    scheduledExecutorService.schedule(() -> doAuth(role), refreshDelayOnFailure, MILLISECONDS);
                    return null;
                });
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
