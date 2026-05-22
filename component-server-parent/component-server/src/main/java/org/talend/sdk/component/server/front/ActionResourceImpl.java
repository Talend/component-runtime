/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.server.api.ActionResource;
import org.talend.sdk.component.server.dao.ComponentActionDao;
import org.talend.sdk.component.server.extension.api.action.Action;
import org.talend.sdk.component.server.front.model.ActionItem;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.front.security.SecurityUtils;
import org.talend.sdk.component.server.service.ExtensionComponentMetadataManager;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;
import org.talend.sdk.component.server.service.httpurlconnection.IgnoreNetAuthenticator;
import org.talend.sdk.component.server.service.jcache.FrontCacheKeyGenerator;
import org.talend.sdk.component.server.service.jcache.FrontCacheResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@IgnoreNetAuthenticator
@CacheDefaults(cacheResolverFactory = FrontCacheResolver.class, cacheKeyGenerator = FrontCacheKeyGenerator.class)
public class ActionResourceImpl implements ActionResource {

    @Inject
    private ComponentManager manager;

    @Inject
    private ComponentActionDao actionDao;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private LocaleMapper localeMapper;

    @Inject
    private ExtensionComponentMetadataManager virtualActions;

    @Inject
    @Context
    private HttpHeaders headers;

    @Inject
    private SecurityUtils secUtils;

    @Override
    public CompletionStage<Response> execute(final String family, final String type, final String action,
            final String lang, final Map<String, String> params) {
        return virtualActions
                .getAction(family, type, action)
                .map(it -> it.getHandler().apply(params, lang).exceptionally(this::onError))
                .orElseGet(() -> doExecuteLocalAction(family, type, action, lang, params));
    }

    @Override
    @CacheResult
    public ActionList getIndex(final String[] types, final String[] families, final String language) {
        final Predicate<String> typeMatcher = new Predicate<String>() {

            private final Collection<String> accepted = new HashSet<>(asList(types));

            @Override
            public boolean test(final String type) {
                return accepted.isEmpty() || accepted.contains(type);
            }
        };
        final Predicate<String> componentMatcher = new Predicate<String>() {

            private final Collection<String> accepted = new HashSet<>(asList(families));

            @Override
            public boolean test(final String family) {
                return accepted.isEmpty() || accepted.contains(family);
            }
        };
        final Locale locale = localeMapper.mapLocale(language);
        return new ActionList(Stream
                .concat(findDeployedActions(typeMatcher, componentMatcher, locale),
                        findVirtualActions(typeMatcher, componentMatcher, locale))
                .collect(toList()));
    }

    private CompletableFuture<Response> doExecuteLocalAction(final String family, final String type,
            final String action, final String lang, final Map<String, String> params) {
        return CompletableFuture.supplyAsync(() -> {
            if (action == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorPayload(ErrorDictionary.ACTION_MISSING, "Action can't be null"))
                        .build());
            }
            if (type == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorPayload(ErrorDictionary.TYPE_MISSING, "Type can't be null"))
                        .build());
            }
            if (family == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorPayload(ErrorDictionary.FAMILY_MISSING, "Family can't be null"))
                        .build());
            }
            final ServiceMeta.ActionMeta actionMeta = actionDao.findBy(family, type, action);
            if (actionMeta == null) {
                throw new WebApplicationException(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.ACTION_MISSING, "No action with id '" + action + "'"))
                        .build());
            }
            try {
                final Map<String, String> runtimeParams = ofNullable(params).map(HashMap::new).orElseGet(HashMap::new);
                runtimeParams.put("$lang", localeMapper.mapLocale(lang).getLanguage());
                String tenant;
                try {
                    tenant = headers.getHeaderString("x-talend-tenant-id");
                } catch (Exception e) {
                    log.debug("[doExecuteLocalAction] context not applicable: {}", e.getMessage());
                    tenant = null;
                }
                final Map<String, String> deciphered = secUtils.decrypt(actionMeta.getParameters()
                        .get(), runtimeParams, tenant);
                final Object result = actionMeta.getInvoker().apply(deciphered);
                return Response.ok(result).type(APPLICATION_JSON_TYPE).build();
            } catch (final RuntimeException re) {
                return onError(re);
            }
            // synchronous, if needed we can move to async with timeout later but currently we don't want.
            // check org.talend.sdk.component.server.service.ComponentManagerService.readCurrentLocale if you change it
        }, Runnable::run).exceptionally(e -> {
            final Throwable cause;
            if (ExecutionException.class.isInstance(e.getCause())) {
                cause = e.getCause().getCause();
            } else {
                cause = e.getCause();
            }
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
                    throw new WebApplicationException(message,
                            Response
                                    .status(response.getStatus())
                                    .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, message))
                                    .build());
                }
            }
            throw new WebApplicationException(Response
                    .status(500)
                    .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, cause.getMessage()))
                    .build());
        });
    }

    private Response onError(final Throwable re) {
        log.warn(re.getMessage(), re);
        if (WebApplicationException.class.isInstance(re.getCause())) {
            return WebApplicationException.class.cast(re.getCause()).getResponse();
        }

        if (ComponentException.class.isInstance(re)) {
            final ComponentException ce = (ComponentException) re;
            throw new WebApplicationException(Response
                    .status(ce.getErrorOrigin() == ComponentException.ErrorOrigin.USER ? 400
                            : ce.getErrorOrigin() == ComponentException.ErrorOrigin.BACKEND ? 456 : 520,
                            "Unexpected callback error")
                    .entity(new ErrorPayload(ErrorDictionary.ACTION_ERROR,
                            "Action execution failed with: " + ofNullable(re.getMessage())
                                    .orElseGet(() -> NullPointerException.class.isInstance(re) ? "unexpected null"
                                            : "no error message")))
                    .build());
        }

        throw new WebApplicationException(Response
                .status(520, "Unexpected callback error")
                .entity(new ErrorPayload(ErrorDictionary.ACTION_ERROR,
                        "Action execution failed with: " + ofNullable(re.getMessage())
                                .orElseGet(() -> NullPointerException.class.isInstance(re) ? "unexpected null"
                                        : "no error message")))
                .build());
    }

    private Stream<ActionItem> findVirtualActions(final Predicate<String> typeMatcher,
            final Predicate<String> componentMatcher, final Locale locale) {
        return virtualActions
                .getActions()
                .stream()
                .filter(act -> typeMatcher.test(act.getReference().getType())
                        && componentMatcher.test(act.getReference().getFamily()))
                .map(Action::getReference)
                .map(it -> new ActionItem(it.getFamily(), it.getType(), it.getName(), it.getProperties()));
    }

    private Stream<ActionItem> findDeployedActions(final Predicate<String> typeMatcher,
            final Predicate<String> componentMatcher, final Locale locale) {
        return manager
                .find(c -> c
                        .get(ContainerComponentRegistry.class)
                        .getServices()
                        .stream()
                        .map(s -> s.getActions().stream())
                        .flatMap(identity())
                        .filter(act -> typeMatcher.test(act.getType()) && componentMatcher.test(act.getFamily()))
                        .map(s -> new ActionItem(s.getFamily(), s.getType(), s.getAction(),
                                propertiesService
                                        .buildProperties(s.getParameters().get(), c.getLoader(), locale, null)
                                        .collect(toList()))));
    }
}
