/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.repository.Config;
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
            final ComponentFamilyMeta.BaseMeta<Object> meta) {
        final Set<ActionReference> actions = getActionReference(meta);
        return findActions(family, actions, container, locale);
    }

    public Collection<ActionReference> findActions(final String family, final Container container, final Locale locale,
            final Config config) {
        final Set<ActionReference> actions =
                getActionReference(toStream(Collections.singleton(config.getMeta())), family);
        return findActions(family, actions, container, locale);
    }

    public Set<ActionReference> getActionReference(final ComponentFamilyMeta.BaseMeta<Object> meta) {
        return getActionReference(toStream(meta.getParameterMetas()), meta.getParent().getName());
    }

    public Set<ActionReference> getActionReference(final Stream<ParameterMeta> parameters, final String familyName) {
        return parameters
                .flatMap(p -> p.getMetadata().entrySet().stream())
                .filter(e -> e.getKey().startsWith(ActionParameterEnricher.META_PREFIX))
                .map(e -> new ActionReference(familyName, e.getValue(),
                        e.getKey().substring(ActionParameterEnricher.META_PREFIX.length()), null))
                .collect(toSet());
    }

    private Collection<ActionReference> findActions(final String family, final Set<ActionReference> actions,
            final Container container, final Locale locale) {
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
                        propertiesService
                                .buildProperties(s.getParameters(), container.getLoader(), locale, null)
                                .collect(toList())))
                .collect(toList());
    }

    private Stream<ParameterMeta> toStream(final Collection<ParameterMeta> parameterMetas) {
        return Stream.concat(parameterMetas.stream(),
                parameterMetas.stream().map(ParameterMeta::getNestedParameters).filter(Objects::nonNull).flatMap(
                        this::toStream));
    }
}
