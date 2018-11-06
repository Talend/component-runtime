/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.talend.sdk.component.server.lang.CustomCollectors.toLinkedMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import org.talend.sdk.component.runtime.internationalization.ParameterBundle;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class PropertiesService {

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    @Inject
    private PropertyValidationService propertyValidationService;

    private Jsonb defaultMapper;

    @PostConstruct
    private void init() {
        defaultMapper =
                JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
    }

    @PreDestroy
    private void destroy() {
        try {
            defaultMapper.close();
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Stream<SimplePropertyDefinition> buildProperties(final List<ParameterMeta> meta, final ClassLoader loader,
            final Locale locale, final DefaultValueInspector.Instance rootInstance) {
        return buildProperties(meta, loader, locale, rootInstance, null);
    }

    private Stream<SimplePropertyDefinition> buildProperties(final List<ParameterMeta> meta, final ClassLoader loader,
            final Locale locale, final DefaultValueInspector.Instance rootInstance, final ParameterMeta parent) {
        return meta.stream().flatMap(p -> {
            final String path = sanitizePropertyName(p.getPath());
            final String name = sanitizePropertyName(p.getName());
            final String type = p.getType().name();
            final boolean isEnum = p.getType() == ParameterMeta.Type.ENUM;
            PropertyValidation validation = propertyValidationService.map(p.getMetadata());
            if (isEnum) {
                if (validation == null) {
                    validation = new PropertyValidation();
                }
                validation.setEnumValues(p.getProposals());
            }
            final Map<String, String> sanitizedMetadata = ofNullable(p.getMetadata())
                    .map(m -> m
                            .entrySet()
                            .stream()
                            .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                            .collect(toLinkedMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue)))
                    .orElse(null);
            final Map<String, String> metadata;
            if (parent != null) {
                metadata = sanitizedMetadata;
            } else {
                metadata = ofNullable(sanitizedMetadata).orElseGet(HashMap::new);
                metadata.put("definition::parameter::index", String.valueOf(meta.indexOf(p)));
            }
            final DefaultValueInspector.Instance instance = defaultValueInspector
                    .createDemoInstance(
                            ofNullable(rootInstance).map(DefaultValueInspector.Instance::getValue).orElse(null), p);
            final ParameterBundle bundle = p.findBundle(loader, locale);
            final ParameterBundle parentBundle = parent == null ? null : parent.findBundle(loader, locale);
            return Stream
                    .concat(Stream
                            .of(new SimplePropertyDefinition(path, name,
                                    bundle.displayName(parentBundle).orElse(p.getName()), type, toDefault(instance, p),
                                    validation, rewriteMetadataForLocale(metadata, parentBundle, bundle),
                                    bundle.placeholder(parentBundle).orElse(p.getName()),
                                    !isEnum ? null
                                            : p
                                                    .getProposals()
                                                    .stream()
                                                    .collect(toLinkedMap(identity(),
                                                            key -> bundle
                                                                    .enumDisplayName(parentBundle, key)
                                                                    .orElse(key))))),
                            buildProperties(p.getNestedParameters(), loader, locale, instance, p));
        }).sorted(Comparator.comparing(SimplePropertyDefinition::getPath)); // important cause it is the way you want to
        // see it
    }

    private Map<String, String> rewriteMetadataForLocale(final Map<String, String> metadata,
            final ParameterBundle parentBundle, final ParameterBundle bundle) {
        final String defaultDoc = metadata.get("documentation::value");
        final String bundleDoc = bundle.documentation(parentBundle).orElse(null);
        if (bundleDoc == null || bundleDoc.equals(defaultDoc)) {
            return metadata;
        }
        final Map<String, String> copy = new HashMap<>(metadata);
        copy.put("documentation::value", bundleDoc);
        return copy;
    }

    private String toDefault(final DefaultValueInspector.Instance instance, final ParameterMeta p) {
        if (instance.isCreated()) {
            return null;
        }
        if (Collection.class.isInstance(instance.getValue()) || Map.class.isInstance(instance.getValue())) {
            // @Experimental("not primitives are a challenge, for now use that but can change if not adapted")
            return defaultMapper.toJson(instance.getValue());
        }
        return defaultValueInspector.findDefault(instance.getValue(), p);
    }

    private String sanitizePropertyName(final String path) {
        return path.replace("${index}", "");
    }
}
