/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
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
import org.talend.sdk.component.server.service.ExtensionComponentMetadataManager;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;
import org.talend.sdk.component.server.service.httpurlconnection.IgnoreNetAuthenticator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@IgnoreNetAuthenticator
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

    @Override
    public CompletionStage<Response> execute(final String family, final String type, final String action,
            final String lang, final Map<String, String> params) {
        return virtualActions
                .getAction(family, type, action)
                .map(it -> it.getHandler().apply(params, lang).exceptionally(this::onError))
                .orElseGet(() -> doExecuteLocalAction(family, type, action, lang, params));
    }

    @Override
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
                final Object result = actionMeta.getInvoker().apply(runtimeParams);
                return Response.ok(result).type(APPLICATION_JSON_TYPE).build();
            } catch (final RuntimeException re) {
                return onError(re);
            }
            // synchronous, if needed we can move to async with timeout later but currently we don't want.
            // check org.talend.sdk.component.server.service.ComponentManagerService.readCurrentLocale if you change it
        }, Runnable::run);
    }

    private Response onError(final Throwable re) {
        log.warn(re.getMessage(), re);
        if (WebApplicationException.class.isInstance(re.getCause())) {
            return WebApplicationException.class.cast(re.getCause()).getResponse();
        }

        if (ComponentException.class.isInstance(re)) {
            ComponentException ce = (ComponentException) re;
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
