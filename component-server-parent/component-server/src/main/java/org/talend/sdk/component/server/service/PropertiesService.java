/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.server.lang.CustomCollectors.toLinkedMap;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.runtime.internationalization.ParameterBundle;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.runtime.manager.util.DefaultValueInspector;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
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

    public void validate(final ConfigTypeNode configNode, final JsonObject payload) {
        ReflectionService.checkWithPayload(buildParameterMetas(configNode.getProperties()),
                extractConfig(configNode, payload), payload);
    }

    private JsonPointer toPointer(final String absoluteTargetPath) {
        return JsonProvider.provider().createPointer('/' + absoluteTargetPath.replace('.', '/'));
    }


    //Map<full path, value> from JsonObject(payload).
    private Map<String, String> extractConfig(final ConfigTypeNode configNode, final JsonObject payload) {
        Map<String, String> payloadConfig = new HashMap<>();
        for (SimplePropertyDefinition propertyDefinition : configNode.getProperties()) {
            try {
                JsonValue value = toPointer(propertyDefinition.getPath()).getValue(payload);
                if (value != null && !JsonObject.class.isInstance(value)) {
                    payloadConfig.put(propertyDefinition.getPath(), getValue(value));
                }
            } catch (JsonException e) {
                continue;
            }
        }

        return payloadConfig;
    }

    private String getValue(final JsonValue value) {
        switch (value.getValueType()) {
            case TRUE:
            case FALSE:
            case NUMBER:
                return String.valueOf(value);
            case STRING:
                return JsonString.class.cast(value).getString();
            default:
                return null;
        }
    }

    private List<ParameterMeta> buildParameterMetas(final List<SimplePropertyDefinition> properties) {
        List<ParameterMeta> parameterMetaList = new ArrayList<>();
        for (SimplePropertyDefinition propertyDefinition : properties) {
            final String path = sanitizePropertyName(propertyDefinition.getPath());
            final String name = sanitizePropertyName(propertyDefinition.getName());
            final ParameterMeta.Type type = findType(propertyDefinition.getType());
            //translate PropertyValidation
            final Map<String, String> sanitizedMetadata = ofNullable(getParamMetadata(propertyDefinition.getMetadata()))
                    .orElse(new LinkedHashMap<>());
            if (propertyDefinition.getValidation() != null) {
                sanitizedMetadata.putAll(propertyValidationService.mapMeta(propertyDefinition.getValidation()));
            }

            parameterMetaList.add(new ParameterMeta(null, null, type, path, name, null,
                    emptyList(), null, sanitizedMetadata, false));
        }
        return parameterMetaList;
    }

    private static LinkedHashMap<String, String> getParamMetadata(final Map<String, String> p) {
        return ofNullable(p)
                .map(m -> m
                        .entrySet()
                        .stream()
                        .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                        .collect(toLinkedMap(e -> e.getKey().replace("condition::", "tcomp::condition::"), Map.Entry::getValue)))
                .orElse(null);
    }

    private static LinkedHashMap<String, String> getSanitizedMetadata(final Map<String, String> p) {
        return ofNullable(p)
                .map(m -> m
                        .entrySet()
                        .stream()
                        .filter(e -> !e.getKey().startsWith(ValidationParameterEnricher.META_PREFIX))
                        .collect(toLinkedMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue)))
                .orElse(null);
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
            final Map<String, String> sanitizedMetadata = getSanitizedMetadata(p.getMetadata());
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
        return Stream
                .concat(metadata.entrySet().stream().filter(shouldBeRewritten.negate()),
                        metadata
                                .entrySet()
                                .stream()
                                .filter(shouldBeRewritten)
                                .map(it -> new AbstractMap.SimpleEntry<>(bundle
                                        .gridLayoutName(parentBundle,
                                                it
                                                        .getKey()
                                                        .substring("ui::gridlayout::".length(),
                                                                it.getKey().length() - "::value".length()))
                                        .map(t -> "ui::gridlayout::" + t + "::value")
                                        .orElse(it.getKey()), it.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    private ParameterMeta.Type findType(final String type) {
        if (ParameterMeta.Type.STRING.name().equals(type)) {
            return ParameterMeta.Type.STRING;
        } else if (ParameterMeta.Type.BOOLEAN.name().equals(type)) {
            return ParameterMeta.Type.BOOLEAN;
        } else if (ParameterMeta.Type.NUMBER.name().equals(type)) {
            return ParameterMeta.Type.NUMBER;
        } else if (ParameterMeta.Type.ENUM.name().equals(type)) {
            return ParameterMeta.Type.ENUM;
        } else if (ParameterMeta.Type.ARRAY.name().equals(type)) {
            return ParameterMeta.Type.ARRAY;
        } else if (ParameterMeta.Type.OBJECT.name().equals(type)) {
            return ParameterMeta.Type.OBJECT;
        }
        return ParameterMeta.Type.OBJECT;
    }
}
