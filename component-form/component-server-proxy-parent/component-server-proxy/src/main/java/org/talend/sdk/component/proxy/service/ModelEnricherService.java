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

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
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

    private final Patches skip = new Patches(null) {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{SKIPPED}";
        }
    };

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Inject
    private ProxyConfiguration configuration;

    private final ConcurrentMap<String, Patches> patches = new ConcurrentHashMap<>();

    private final Function<String, ResourceBundle> bundleSupplier =
            lang -> ResourceBundle.getBundle("org.talend.sdk.component.proxy.enrichment.i18n.Messages",
                    new Locale(lang), Thread.currentThread().getContextClassLoader());

    public ConfigTypeNode enrich(final ConfigTypeNode node, final String lang) {
        return ofNullable(doEnrich(node.getConfigurationType(), lang,
                () -> new ConfigTypeNode(node.getId(), node.getVersion(), node.getParentId(),
                        node.getConfigurationType(), node.getName(), node.getDisplayName(),
                        new HashSet<>(node.getEdges()), new ArrayList<>(node.getProperties()),
                        new ArrayList<>(node.getActions())),
                ConfigTypeNode::getProperties)).orElse(node);
    }

    public ComponentDetail enrich(final ComponentDetail node, final String lang) {
        return ofNullable(doEnrich("component", lang,
                () -> new ComponentDetail(node.getId(), node.getDisplayName(), node.getIcon(), node.getType(),
                        node.getVersion(), new ArrayList<>(node.getProperties()), new ArrayList<>(node.getActions()),
                        new ArrayList<>(node.getInputFlows()), new ArrayList<>(node.getOutputFlows()),
                        new ArrayList<>(node.getLinks())),
                ComponentDetail::getProperties)).orElse(node);
    }

    private <T> T doEnrich(final String type, final String lang, final Supplier<T> lightCopyProvider,
            final Function<T, Collection<SimplePropertyDefinition>> propertiesExtractor) {
        final Patches config =
                patches.computeIfAbsent(type, k -> findPatch(type).orElseGet(() -> findPatch("default").orElse(skip)));
        if (config == skip) {
            return null;
        }
        final Patch patch = config.forLang(bundleSupplier.apply(lang));
        final T copy = lightCopyProvider.get();
        final Collection<SimplePropertyDefinition> properties = propertiesExtractor.apply(copy);
        ofNullable(patch.getProperties()).ifPresent(properties::addAll);
        return copy;
    }

    private Optional<Patches> findPatch(final String type) {
        return Optional
                .of(String.format(configuration.getUiSpecPatchLocation(), type))
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
        p.setProperties(ofNullable(p.getProperties()).orElseGet(Collections::emptyList));
        p.getProperties().forEach(prop -> {
            if (prop.getProposalDisplayNames() != null) {
                if (prop.getValidation() == null) {
                    prop.setValidation(new PropertyValidation());
                }
                if (prop.getValidation().getEnumValues() == null) {
                    prop.getValidation().setEnumValues(prop.getProposalDisplayNames().keySet());
                }
            } else if (prop.getValidation() != null && prop.getValidation().getEnumValues() != null
                    && prop.getProposalDisplayNames() == null) {
                prop.setProposalDisplayNames(
                        prop.getValidation().getEnumValues().stream().collect(toMap(identity(), identity())));
            }

            if (prop.getMetadata() == null) {
                prop.setMetadata(emptyMap());
            }

            if (prop.getName() == null) {
                prop.setName(prop.getPath().substring(prop.getPath().lastIndexOf('.') + 1));
            }
        });
        return p;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Patch {

        private Collection<SimplePropertyDefinition> properties;
    }

    @RequiredArgsConstructor
    private static class Patches {

        private final Patch base;

        private final ConcurrentMap<String, Patch> patchPerLang = new ConcurrentHashMap<>();

        private Patch forLang(final ResourceBundle bundle) {
            return patchPerLang
                    .computeIfAbsent(bundle.getLocale().getLanguage(),
                            k -> new Patch(
                                    base.properties
                                            .stream()
                                            .map(p -> new SimplePropertyDefinition(p.getPath(),
                                                    findTranslation(bundle, p.getName()),
                                                    findTranslation(bundle, p.getDisplayName()), p.getType(),
                                                    p.getDefaultValue(), p.getValidation(),
                                                    ofNullable(p.getMetadata())
                                                            .map(m -> m.entrySet().stream().collect(toMap(
                                                                    Map.Entry::getKey,
                                                                    e -> findTranslation(bundle, e.getValue()))))
                                                            .orElse(null),
                                                    findTranslation(bundle, p.getPlaceholder()),
                                                    ofNullable(p.getProposalDisplayNames())
                                                            .map(proposals -> proposals
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(toMap(Map.Entry::getKey,
                                                                            e -> findTranslation(bundle,
                                                                                    e.getValue()))))
                                                            .orElse(null)))
                                            .collect(toList())));
        }

        private String findTranslation(final ResourceBundle bundle, final String keyOrValue) {
            if (keyOrValue == null) {
                return null;
            }
            return bundle.containsKey(keyOrValue) ? bundle.getString(keyOrValue) : keyOrValue;
        }
    }
}
