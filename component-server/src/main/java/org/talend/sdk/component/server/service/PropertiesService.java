/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class PropertiesService {

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    @Inject
    private PropertyValidationService propertyValidationService;

    public Stream<SimplePropertyDefinition> buildProperties(final Collection<ParameterMeta> meta,
            final ClassLoader loader, final Locale locale, final Object rootInstance) {
        return buildProperties(meta, loader, locale, rootInstance, null);
    }

    private Stream<SimplePropertyDefinition> buildProperties(final Collection<ParameterMeta> meta,
            final ClassLoader loader, final Locale locale, final Object rootInstance, final ParameterMeta parent) {
        return meta.stream().flatMap(p -> {
            final String path = sanitizePropertyName(p.getPath());
            final String name = sanitizePropertyName(p.getName());
            final String type = p.getType().name();
            PropertyValidation validation = propertyValidationService.map(p.getMetadata());
            if (p.getType() == ParameterMeta.Type.ENUM) {
                if (validation == null) {
                    validation = new PropertyValidation();
                }
                validation.setEnumValues(p.getProposals());
            }
            final Map<String, String> metadata = ofNullable(p.getMetadata()).map(m -> m.entrySet().stream()
                    .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                    .collect(toMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue))).orElse(null);
            final Object instance = defaultValueInspector.createDemoInstance(rootInstance, p);
            return Stream.concat(
                    Stream.of(new SimplePropertyDefinition(path, name,
                            p.findBundle(loader, locale).displayName()
                                    .orElseGet(() -> parent == null ? name
                                            : parent.findBundle(loader, locale).displayName(p.getName()).orElse(name)),
                            type, defaultValueInspector.findDefault(instance, p), validation, metadata)),
                    buildProperties(p.getNestedParameters(), loader, locale, instance, p));
        }).sorted(Comparator.comparing(SimplePropertyDefinition::getPath)); // important cause it is the way you want to
        // see it
    }

    private String sanitizePropertyName(final String path) {
        return path.replace("${index}", "");
    }
}
