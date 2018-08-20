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
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.api.integration.application.ReferenceService;
import org.talend.sdk.component.proxy.api.integration.application.Values;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@ApplicationScoped
public class PropertiesService {

    @Inject
    private ConfigurationClient configurations;

    @Inject
    private ReferenceService referenceService;

    /**
     * Take an object properties list and replace the nested configurations
     * by a xxx$reference STRING (id).
     *
     * @param properties the properties to filter.
     * @param context current conversion context.
     * @return the new properties list.
     */
    public CompletionStage<List<SimplePropertyDefinition>>
            filterProperties(final Collection<SimplePropertyDefinition> properties, final UiSpecContext context) {
        final Collection<SimplePropertyDefinition> nestedConfigurations = dropChildren(properties
                .stream()
                .filter(this::isConfiguration)
                .filter(it -> !it.getName().equals(it.getPath()) /* root is the one we convert */)
                .map(ref -> {
                    final Map<String, String> enrichedMetadata = new HashMap<>(ref.getMetadata());
                    // note: we can move to suggestions later
                    enrichedMetadata.put("action::dynamic_values",
                            "builtin::references(type=" + ref.getMetadata().get("configurationtype::type") + ",name="
                                    + ref.getMetadata().get("configurationtype::name") + ")");

                    return new SimplePropertyDefinition(ref.getPath(), ref.getName(), ref.getDisplayName(), "STRING",
                            null,
                            new PropertyValidation(true, null, null, null, null, null, null, null, null, emptySet()),
                            enrichedMetadata, null, emptyMap());
                })
                .collect(toList()));

        final CompletionStage<Void> fillMeta;
        if (!nestedConfigurations.isEmpty()) {
            fillMeta = configurations
                    .getAllConfigurations(context.getLanguage(), context.getPlaceholderProvider())
                    .thenApply(nodes -> nestedConfigurations.stream().map(it -> {
                        final String type = it.getMetadata().getOrDefault("configurationtype::type", "");
                        final String name = it.getMetadata().getOrDefault("configurationtype::name", "default");
                        return nodes
                                .getNodes()
                                .values()
                                .stream()
                                .filter(node -> type.equals(node.getConfigurationType()) && name.equals(node.getName()))
                                .findFirst()
                                .map(node -> findPotentialIdsAndNames(node, context))
                                .orElseGet(() -> completedFuture(emptyMap()))
                                .thenApply(potentialIdsAndNames -> {
                                    it.getValidation().setEnumValues(potentialIdsAndNames.keySet());
                                    it.setProposalDisplayNames(potentialIdsAndNames);
                                    return null;
                                });
                    }).toArray(CompletableFuture[]::new))
                    .thenCompose(CompletableFuture::allOf);
        } else {
            fillMeta = completedFuture(null);
        }

        return fillMeta.thenApply(ignored -> Stream
                .concat(properties
                        .stream()
                        .filter(it -> nestedConfigurations
                                .stream()
                                .noneMatch(prefix -> prefix.getPath().equals(it.getPath())
                                        || it.getPath().startsWith(prefix.getPath() + '.'))),
                        // not the best naming convention but enough for now
                        nestedConfigurations.stream().peek(it -> it.setPath(it.getPath() + ".$selfReference")))
                .collect(toList()));
    }

    private boolean isConfiguration(final SimplePropertyDefinition it) {
        return it.getMetadata().containsKey("configurationtype::name")
                && it.getMetadata().containsKey("configurationtype::type");
    }

    private List<SimplePropertyDefinition> dropChildren(final List<SimplePropertyDefinition> collect) {
        return collect
                .stream()
                .filter(it -> collect.stream().noneMatch(parent -> it.getPath().startsWith(parent.getPath() + '.')))
                .collect(toList());
    }

    private CompletionStage<Map<String, String>> findPotentialIdsAndNames(final ConfigTypeNode node,
            final UiSpecContext context) {
        return referenceService
                .findReferencesByTypeAndName(node.getConfigurationType(), node.getId(), context)
                .thenApply(values -> ofNullable(values.getItems())
                        .map(items -> items.stream().collect(toMap(Values.Item::getId, Values.Item::getLabel)))
                        .orElseGet(Collections::emptyMap));
    }

    public CompletionStage<Map<String, String>> replaceReferences(final UiSpecContext context,
            final List<SimplePropertyDefinition> properties, final Map<String, String> instance) {
        if (instance == null) {
            return completedFuture(emptyMap());
        }
        final List<SimplePropertyDefinition> references = properties
                .stream()
                .filter(it -> it.getPath().endsWith(".$selfReference"))
                .filter(it -> !instance.getOrDefault(it.getPath(), "").isEmpty())
                .collect(toList());
        if (references.isEmpty()) {
            return completedFuture(instance);
        }

        // impl note: for now we don't support references in arrays so we only match exact path,
        // if we start to get configs with storable arrays we'll need to update that
        final Map<String, String> config = instance
                .entrySet()
                .stream()
                .filter(it -> references.stream().noneMatch(ref -> ref.getPath().equals(it.getKey())))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return allOf(references.stream().map(it -> {
            final String id = instance.get(it.getPath());
            if (id == null || id.trim().isEmpty()) {
                return completedFuture(null);
            }
            final String sanitizedPath = it.getPath().replace(".$selfReference", "");
            ofNullable(instance.get(it.getPath())).ifPresent(val -> config.put(it.getPath(), val));
            return referenceService.findPropertiesById(id, context).thenCompose(form -> configurations
                    .getDetails(context.getLanguage(), form.getFormId(), context.getPlaceholderProvider())
                    .thenCompose(detail -> filterProperties(detail.getProperties(), context).thenCompose(
                            props -> replaceReferences(context, props, form.getProperties()).thenApply(nestedProps -> {
                                final String root = detail
                                        .getProperties()
                                        .stream()
                                        .map(SimplePropertyDefinition::getPath)
                                        .sorted()
                                        .iterator()
                                        .next();
                                config.putAll(nestedProps
                                        .entrySet()
                                        .stream()
                                        .collect(toMap(e -> e.getKey().startsWith(root)
                                                ? sanitizedPath + e.getKey().substring(root.length())
                                                : e.getKey(), Map.Entry::getValue)));
                                return null;
                            }))));
        }).toArray(CompletableFuture[]::new)).thenApply(ignored -> config);
    }
}
