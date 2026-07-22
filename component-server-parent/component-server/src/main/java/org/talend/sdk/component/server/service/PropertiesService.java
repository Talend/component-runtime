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
package org.talend.sdk.component.server.service;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.server.lang.CustomCollectors.toLinkedMap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.internationalization.ParameterBundle;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.service.qualifier.ComponentServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class PropertiesService {

    private final DefaultValueInspector defaultValueInspector = new DefaultValueInspector();

    @Inject
    private PropertyValidationService propertyValidationService;

    @Inject
    private ComponentServerConfiguration componentServerConfiguration;

    @Inject
    @ComponentServer
    private Jsonb defaultMapper;

    public Stream<SimplePropertyDefinition> buildProperties(final List<ParameterMeta> meta, final ClassLoader loader,
            final Locale locale, final DefaultValueInspector.Instance rootInstance) {
        return buildProperties(meta, loader, locale, rootInstance, null);
    }

    private Stream<SimplePropertyDefinition> buildProperties(final List<ParameterMeta> meta, final ClassLoader loader,
            final Locale locale, final DefaultValueInspector.Instance rootInstance, final ParameterMeta parent) {
        return meta.stream()
                .flatMap(p -> buildProperty(p, meta, loader, locale, rootInstance, parent))
                // important cause it is the way you want to see it
                .sorted(Comparator.comparing(SimplePropertyDefinition::getPath));
    }

    private Stream<SimplePropertyDefinition> buildProperty(final ParameterMeta p,
            final List<ParameterMeta> siblings,
            final ClassLoader loader,
            final Locale locale,
            final DefaultValueInspector.Instance rootInstance,
            final ParameterMeta parent) {
        final String path = sanitizePropertyName(p.getPath());
        final String name = sanitizePropertyName(p.getName());
        final String type = p.getType().name();

        final PropertyValidation validation = buildValidation(p);
        final Map<String, String> metadata = buildMetadata(p, siblings, parent);

        final DefaultValueInspector.Instance instance = defaultValueInspector
                .createDemoInstance(ofNullable(rootInstance)
                        .map(DefaultValueInspector.Instance::getValue)
                        .orElse(null), p);
        final ParameterBundle bundle = p.findBundle(loader, locale);
        final ParameterBundle parentBundle = parent == null ? null : parent.findBundle(loader, locale);
        final String displayName = bundle.displayName(parentBundle).orElse(p.getName());
        final String placeholder = bundle.placeholder(parentBundle).orElse(p.getName());

        final LinkedHashMap<String, String> enumValues = buildEnumDisplayNames(p, bundle, parentBundle);
        final SimplePropertyDefinition def = new SimplePropertyDefinition(path, name, displayName, type,
                toDefault(instance, p), validation, rewriteMetadataForLocale(metadata, parentBundle, bundle),
                placeholder, enumValues);

        return Stream.concat(
                Stream.of(def),
                buildProperties(p.getNestedParameters(), loader, locale, instance, p));
    }

    private PropertyValidation buildValidation(final ParameterMeta p) {
        PropertyValidation validation = propertyValidationService.map(p.getMetadata());
        if (p.getType() != ParameterMeta.Type.ENUM) {
            return validation;
        }
        if (validation == null) {
            validation = new PropertyValidation();
        }
        validation.setEnumValues(p.getProposals());
        return validation;
    }

    private Map<String, String> buildMetadata(
            final ParameterMeta p,
            final List<ParameterMeta> siblings,
            final ParameterMeta parent) {
        final Map<String, String> sanitized = ofNullable(p.getMetadata())
                .map(m -> m
                        .entrySet()
                        .stream()
                        .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                        .collect(toLinkedMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue)))
                .orElse(null);
        if (parent != null) {
            return sanitized;
        }

        final Map<String, String> metadata = ofNullable(sanitized).orElseGet(HashMap::new);
        metadata.put("definition::parameter::index", String.valueOf(siblings.indexOf(p)));
        // this one to mark the Schema parameter somehow to differentiate it from the branch name
        if (p.getJavaType() instanceof Class<?> clazzType && Schema.class.isAssignableFrom(clazzType)) {
            metadata.put("definition::parameter::schema", "");
        }
        return metadata;
    }

    private LinkedHashMap<String, String> buildEnumDisplayNames(final ParameterMeta p, final ParameterBundle bundle,
            final ParameterBundle parentBundle) {
        if (p.getType() != ParameterMeta.Type.ENUM) {
            return null;
        }

        return p.getProposals()
                .stream()
                .collect(toLinkedMap(identity(), key -> bundle.enumDisplayName(parentBundle, key).orElse(key)));
    }

    private Map<String, String> rewriteMetadataForLocale(final Map<String, String> metadata,
            final ParameterBundle parentBundle, final ParameterBundle bundle) {
        return rewriteLayoutMetadata(rewriteDocMetadata(metadata, parentBundle, bundle), parentBundle, bundle);
    }

    private Map<String, String> rewriteDocMetadata(final Map<String, String> metadata,
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

    private Map<String, String> rewriteLayoutMetadata(final Map<String, String> metadata,
            final ParameterBundle parentBundle, final ParameterBundle bundle) {
        if (!componentServerConfiguration.getTranslateGridLayoutTabNames()) {
            return metadata;
        }

        final Collection<String> keysToRewrite = metadata
                .keySet()
                .stream()
                .filter(it -> it.startsWith("ui::gridlayout::") && it.endsWith("::value"))
                .collect(toSet());
        if (keysToRewrite.isEmpty()) {
            return metadata;
        }
        final Predicate<Map.Entry<String, ?>> shouldBeRewritten = k -> keysToRewrite.contains(k.getKey());
        return Stream.concat(
                metadata.entrySet()
                        .stream()
                        .filter(shouldBeRewritten.negate()),
                metadata.entrySet()
                        .stream()
                        .filter(shouldBeRewritten)
                        .map(it -> new AbstractMap.SimpleEntry<>(
                                bundle.gridLayoutName(parentBundle,
                                        it.getKey()
                                                .substring("ui::gridlayout::".length(),
                                                        it.getKey().length() - "::value".length()))
                                        .map(t -> "ui::gridlayout::" + t + "::value")
                                        .orElse(it.getKey()),
                                it.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String toDefault(final DefaultValueInspector.Instance instance, final ParameterMeta p) {
        if (instance.isCreated()) {
            return null;
        }
        if (instance.getValue() instanceof Collection || instance.getValue() instanceof Map) {
            // @Experimental("not primitives are a challenge, for now use that but can change if not adapted")
            return defaultMapper.toJson(instance.getValue());
        }
        return defaultValueInspector.findDefault(instance.getValue(), p);
    }

    private String sanitizePropertyName(final String path) {
        return path.replace("${index}", "");
    }
}
