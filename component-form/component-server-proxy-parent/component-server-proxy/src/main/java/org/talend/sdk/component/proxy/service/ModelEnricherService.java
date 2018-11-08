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
package org.talend.sdk.component.proxy.service;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.JsonValue.ValueType.STRING;
import static javax.json.stream.JsonCollectors.toJsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonString;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ModelEnricherService {

    private static final Collection<ActionReference> BUILTIN_ACTIONS = Stream
            .of(new ActionReference("builtin::family", "builtin::root::reloadFromId", "reloadForm", "reloadForm",
                    new ArrayList<>(singleton(
                            new SimplePropertyDefinition("id", "id", "Configuration Identifier", "STRING", null, null,
                                    singletonMap("definition::parameter::index", "0"), null, new LinkedHashMap<>())))),
                    new ActionReference("builtin::family", "builtin::root::reloadFromParentEntityId",
                            "reloadFromParentEntityId", "reloadFromParentEntityId",
                            new ArrayList<>(singleton(new SimplePropertyDefinition("id", "id",
                                    "Configuration Identifier", "STRING", null, null,
                                    singletonMap("definition::parameter::index", "0"), null, new LinkedHashMap<>())))),
                    new ActionReference("builtin::family", "builtin::roots", "dynamic_values", "roots",
                            new ArrayList<>()),
                    // these ones are configured from the definition (xxxx=yyy) and not the form params
                    new ActionReference("builtin::family", "builtin::http::dynamic_values", "dynamic_values", "http",
                            new ArrayList<>()),
                    new ActionReference("builtin::family", "builtin::references", "suggestions", "references",
                            new ArrayList<>()),
                    new ActionReference("builtin::family", "builtin::childrenTypes", "suggestions", "childrenTypes",
                            new ArrayList<>(singleton(new SimplePropertyDefinition("parentId", "parentId",
                                    "Parent identifier", "STRING", null, null,
                                    singletonMap("definition::parameter::index", "0"), null, new LinkedHashMap<>())))),
                    new ActionReference("builtin::family", "builtin::root::reloadFromParentEntityIdAndType",
                            "reloadFromParentEntityIdAndType", "reloadFromParentEntityIdAndType",
                            new ArrayList<>(singletonList(new SimplePropertyDefinition("$datasetMetadata",
                                    "$datasetMetadata", "$datasetMetadata", "OBJECT", null, null,
                                    singletonMap("definition::parameter::index", "1"), null, new LinkedHashMap<>())))))
            .map(act -> new ActionReference(act.getFamily(), act.getName(), act.getType(), act.getDisplayName(), Stream
                    .concat(act.getProperties().stream(), Stream
                            .of(new SimplePropertyDefinition("$formId", "$formId", "$formId", "STRING", null,
                                    new PropertyValidation(false, null, null, null, null, null, null, null, null, null),
                                    singletonMap("definition::parameter::index",
                                            Integer.toString(act.getProperties().size())),
                                    null, null)))
                    .collect(toList())))
            .collect(toList());

    private final Patches skip = new Patches(null) {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{SKIPPED}";
        }
    };

    @Inject
    @UiSpecProxy
    private JsonBuilderFactory builderFactory;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Inject
    @UiSpecProxy
    private JsonProvider json;

    @Inject
    private ProxyConfiguration configuration;

    private final ConcurrentMap<String, Patches> patches = new ConcurrentHashMap<>();

    private JsonObject emptyPayload;

    @PostConstruct
    private void init() {
        emptyPayload = builderFactory.createObjectBuilder().build();
    }

    private final Function<String, ResourceBundle> bundleSupplier = lang -> {
        final Locale locale = new Locale(lang);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return ResourceBundle
                    .getBundle("org.talend.sdk.component.proxy.enrichment.i18n.Messages", locale, classLoader);
        } catch (final MissingResourceException mre) {
            return new ResourceBundle() {

                @Override
                public Locale getLocale() {
                    return locale;
                }

                @Override // normally not used since getKeys will prevent it in our usage
                protected Object handleGetObject(final String key) {
                    return key;
                }

                @Override
                public Enumeration<String> getKeys() {
                    return emptyEnumeration();
                }
            };
        }
    };

    public ConfigTypeNode enrich(final ConfigTypeNode node, final String lang) {
        return doEnrich(node.getConfigurationType(), lang, patch -> {
            final ConfigTypeNode copy = new ConfigTypeNode(node.getId(), node.getVersion(), node.getParentId(),
                    node.getConfigurationType(), node.getName(), node.getDisplayName(), node.getEdges(),
                    new ArrayList<>(), new ArrayList<>(node.getActions()));
            patch.doPatchProperties(copy.getProperties(), node.getProperties());
            patch.appendBuiltInActions(copy.getActions());
            return copy;
        }).orElse(node);
    }

    public ComponentDetail enrich(final ComponentDetail node, final String lang) {
        return doEnrich("component", lang, patch -> {
            final ComponentDetail copy = new ComponentDetail(node.getId(), node.getDisplayName(), node.getIcon(),
                    node.getType(), node.getVersion(), new ArrayList<>(), new ArrayList<>(node.getActions()),
                    node.getInputFlows(), node.getOutputFlows(), node.getLinks());
            patch.doPatchProperties(copy.getProperties(), node.getProperties());
            patch.appendBuiltInActions(copy.getActions());
            return copy;
        }).orElse(node);
    }

    private <T> Optional<T> doEnrich(final String type, final String lang, final Function<Patch, T> propertiesMerger) {
        final Patches config = getPatch(type);
        if (config == skip) {
            return empty();
        }
        return ofNullable(config.forLang(bundleSupplier.apply(lang), json)).map(propertiesMerger);
    }

    private Patches getPatch(final String type) {
        return patches.computeIfAbsent(type, k -> findPatch(type).orElseGet(() -> findPatch("default").orElse(skip)));
    }

    private Optional<Patches> findPatch(final String type) {
        return ofNullable(configuration.getUiSpecPatchLocation())
                .map(l -> String.format(l, type))
                .flatMap(this::extractLocation)
                .map(stream -> {
                    try {
                        return jsonb.fromJson(stream, Patch.class);
                    } finally {
                        try {
                            stream.close();
                        } catch (final IOException e) {
                            log.warn(e.getMessage(), e);
                        }
                    }
                })
                .map(this::normalize)
                .map(Patches::new);
    }

    private Patch normalize(final Patch p) {
        p.setPrependProperties(ofNullable(p.getPrependProperties()).orElseGet(Collections::emptyList));
        p.getPrependProperties().forEach(this::normalize);
        p.setAppendProperties(ofNullable(p.getAppendProperties()).orElseGet(Collections::emptyList));
        p.getAppendProperties().forEach(this::normalize);
        return p;
    }

    private void normalize(final SimplePropertyDefinition prop) {
        if (prop.getProposalDisplayNames() != null) {
            if (prop.getValidation() == null) {
                prop.setValidation(new PropertyValidation());
            }
            if (prop.getValidation().getEnumValues() == null) {
                prop.getValidation().setEnumValues(prop.getProposalDisplayNames().keySet());
            }
        } else if (prop.getValidation() != null && prop.getValidation().getEnumValues() != null
                && prop.getProposalDisplayNames() == null) {
            prop
                    .setProposalDisplayNames(prop
                            .getValidation()
                            .getEnumValues()
                            .stream()
                            .collect(toMap(identity(), identity(), (a, b) -> {
                                throw new IllegalArgumentException("Conflict is not possible here");
                            }, LinkedHashMap::new)));
        }

        if (prop.getMetadata() == null) {
            prop.setMetadata(emptyMap());
        }

        if (prop.getName() == null) {
            prop.setName(prop.getPath().substring(prop.getPath().lastIndexOf('.') + 1));
        }
    }

    private Optional<InputStream> extractLocation(final String location) {
        final int query = location.indexOf('?');
        if (query > 0) {
            final String queryString = location.substring(query + 1);
            final String rawLocation = location.substring(0, query);
            return Optional.ofNullable(findConfig(rawLocation).orElseGet(() -> {
                if ("force=true".equals(queryString)) {
                    throw new IllegalArgumentException(
                            "No uispec " + rawLocation + "found, if it is acceptable use force=false parameter");
                }
                return null;
            }));
        }
        return findConfig(location);
    }

    private Optional<InputStream> findConfig(final String location) {
        final File file = new File(location);
        if (file.exists()) {
            try {
                return Optional.of(new FileInputStream(location));
            } catch (final FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        final File home = new File(configuration.getHome());
        if (home.exists()) {
            final Optional<File> configFile = Stream
                    .of(location, "conf/" + location)
                    .map(f -> new File(home, f))
                    .filter(File::exists)
                    .findFirst();
            if (configFile.isPresent()) {
                try {
                    return Optional.of(new FileInputStream(configFile.get()));
                } catch (final FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream(location));
    }

    public JsonObject extractEnrichment(final String type, final String lang, final JsonObject payload) {
        return doEnrich(type, lang,
                patch -> Stream
                        .concat(filterRoots(patch.getPrependProperties()), filterRoots(patch.getAppendProperties()))
                        .map(SimplePropertyDefinition::getName)
                        .filter(payload::containsKey)
                        .map(key -> new AbstractMap.SimpleEntry<>(key, payload.get(key)))
                        .collect(toJsonObject())).orElse(emptyPayload);
    }

    public Optional<String> findEnclosedFormId(final String type, final String lang, final JsonObject payload) {
        return doEnrich(type, lang,
                patch -> ofNullable(patch.getTypePointer())
                        .map(it -> it.getValue(payload))
                        .filter(it -> it.getValueType() == STRING)
                        .map(it -> JsonString.class.cast(it).getString())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No form identifier available, check your server configuration.")));
    }

    private Stream<SimplePropertyDefinition> filterRoots(final Collection<SimplePropertyDefinition> props) {
        return ofNullable(props)
                .map(p -> p.stream().filter(it -> it.getName().equals(it.getPath())))
                .orElseGet(Stream::empty);
    }

    // first object where name==path
    public String findPropertyPrefixForEnrichingProperty(final String configurationType) {
        return ofNullable(getPatch(configurationType))
                .filter(it -> it.base != null)
                .map(patch -> Stream
                        .of(patch.base.prependProperties, patch.base.appendProperties)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .filter(it -> "OBJECT".equalsIgnoreCase(it.getType()) && it.getName() != null
                                && it.getName().equals(it.getPath()))
                        .findFirst())
                .flatMap(identity())
                .map(SimplePropertyDefinition::getName)
                .orElse(null);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Patch {

        private Collection<SimplePropertyDefinition> prependProperties;

        private Collection<SimplePropertyDefinition> appendProperties;

        private JsonPointer typePointer;

        private void doPatchProperties(final Collection<SimplePropertyDefinition> properties,
                final Collection<SimplePropertyDefinition> original) {
            ofNullable(prependProperties).ifPresent(properties::addAll);
            ofNullable(original).ifPresent(properties::addAll);
            ofNullable(appendProperties).ifPresent(properties::addAll);
        }

        private void appendBuiltInActions(final Collection<ActionReference> actions) {
            actions.addAll(BUILTIN_ACTIONS);
        }
    }

    @RequiredArgsConstructor
    private static class Patches {

        private final Patch base;

        private final ConcurrentMap<String, Patch> patchPerLang = new ConcurrentHashMap<>();

        private Patch forLang(final ResourceBundle bundle, final JsonProvider provider) {
            return patchPerLang.computeIfAbsent(bundle.getLocale().getLanguage(), k -> {
                final List<SimplePropertyDefinition> prependProperties =
                        withTranslations(bundle, base.prependProperties);
                final List<SimplePropertyDefinition> appendProperties = withTranslations(bundle, base.appendProperties);
                return new Patch(prependProperties, appendProperties,
                        Stream
                                .of(prependProperties, appendProperties)
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .filter(it -> "true".equalsIgnoreCase(it.getMetadata().get("proxyserver::formId"))
                                        && !it.getPath().contains("${index}"))
                                .findFirst()
                                .map(it -> provider.createPointer('/' + it.getPath().replace('.', '/')))
                                .orElse(null));
            });
        }

        private List<SimplePropertyDefinition> withTranslations(final ResourceBundle bundle,
                final Collection<SimplePropertyDefinition> props) {
            return props
                    .stream()
                    .map(p -> new SimplePropertyDefinition(p.getPath(), findTranslation(bundle, p.getName()),
                            findTranslation(bundle, p.getDisplayName()), p.getType(), p.getDefaultValue(),
                            p.getValidation(),
                            ofNullable(p.getMetadata())
                                    .map(m -> m
                                            .entrySet()
                                            .stream()
                                            .collect(toMap(Map.Entry::getKey,
                                                    e -> findTranslation(bundle, e.getValue()))))
                                    .orElse(null),
                            findTranslation(bundle, p.getPlaceholder()),
                            ofNullable(p.getProposalDisplayNames())
                                    .map(proposals -> proposals
                                            .entrySet()
                                            .stream()
                                            .collect(toMap(Map.Entry::getKey,
                                                    e -> findTranslation(bundle, e.getValue()), (a, b) -> {
                                                        throw new IllegalArgumentException("can't happen");
                                                    }, LinkedHashMap::new)))
                                    .orElse(null)))
                    .collect(toList());
        }

        private String findTranslation(final ResourceBundle bundle, final String keyOrValue) {
            if (keyOrValue == null) {
                return null;
            }
            return bundle.containsKey(keyOrValue) ? bundle.getString(keyOrValue) : keyOrValue;
        }
    }
}
