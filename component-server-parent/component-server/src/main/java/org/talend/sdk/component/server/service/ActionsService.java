/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.repository.Config;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.internationalization.FamilyBundle;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ActionParameterEnricher;
import org.talend.sdk.component.server.front.model.ActionReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ActionsService {

    @Inject
    private PropertiesService propertiesService;

    public Collection<ActionReference> findActions(final String family, final Container container, final Locale locale,
            final ComponentFamilyMeta.BaseMeta<Lifecycle> meta, final FamilyBundle familyBundle) {
        final Set<ActionReference> actions = getActionReference(meta, familyBundle);
        return findActions(family, actions, container, locale, familyBundle);
    }

    public Collection<ActionReference> findActions(final String family, final Container container, final Locale locale,
            final Config config, final FamilyBundle familyBundle) {
        final Set<ActionReference> actions =
                getActionReference(toStream(singleton(config.getMeta())), family, familyBundle);
        return findActions(family, actions, container, locale, familyBundle);
    }

    public Set<ActionReference> getActionReference(final ComponentFamilyMeta.BaseMeta<Lifecycle> meta,
            final FamilyBundle familyBundle) {
        return getActionReference(toStream(meta.getParameterMetas().get()), meta.getParent().getName(), familyBundle);
    }

    public Set<ActionReference> getActionReference(final Stream<ParameterMeta> parameters, final String familyName,
            final FamilyBundle familyBundle) {
        return parameters
                .flatMap(p -> p.getMetadata().entrySet().stream())
                .filter(e -> e.getKey().startsWith(ActionParameterEnricher.META_PREFIX))
                .map(e -> {
                    final String type = e.getKey().substring(ActionParameterEnricher.META_PREFIX.length());
                    return new ActionReference(familyName, e.getValue(), type,
                            familyBundle.actionDisplayName(type, e.getValue()).orElse(e.getValue()), null);
                })
                .collect(toSet());
    }

    private Collection<ActionReference> findActions(final String family, final Set<ActionReference> actions,
            final Container container, final Locale locale, final FamilyBundle familyBundle) {
        final ContainerComponentRegistry registry = container.get(ContainerComponentRegistry.class);
        return registry
                .getServices()
                .stream()
                .flatMap(s -> s.getActions().stream())
                .filter(s -> s.getFamily().equals(family))
                .filter(s -> actions
                        .stream()
                        .anyMatch(e -> s.getFamily().equals(e.getFamily()) && s.getType().equals(e.getType())
                                && s.getAction().equals(e.getName())))
                .map(s -> new ActionReference(s.getFamily(), s.getAction(), s.getType(),
                        familyBundle.actionDisplayName(s.getType(), s.getAction()).orElse(s.getAction()),
                        propertiesService
                                .buildProperties(s.getParameters().get(), container.getLoader(), locale, null)
                                .collect(toList())))
                .collect(toList());
    }

    private Stream<ParameterMeta> toStream(final Collection<ParameterMeta> parameterMetas) {
        return Stream
                .concat(parameterMetas.stream(),
                        parameterMetas
                                .stream()
                                .map(ParameterMeta::getNestedParameters)
                                .filter(Objects::nonNull)
                                .flatMap(this::toStream));
    }
}
