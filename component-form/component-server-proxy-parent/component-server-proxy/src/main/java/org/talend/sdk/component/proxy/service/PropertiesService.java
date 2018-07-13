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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.api.persistence.OnFindByFormId;
import org.talend.sdk.component.proxy.api.persistence.OnFindById;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

@ApplicationScoped
public class PropertiesService {

    @Inject
    private ConfigurationClient configurations;

    @Inject
    private Event<OnFindByFormId> findByFormIdEvent;

    @Inject
    private Event<OnFindById> onFindByIdEvent;

    public CompletionStage<List<SimplePropertyDefinition>> filterProperties(final String lang,
            final Function<String, String> placeholders, final Collection<SimplePropertyDefinition> properties) {
        final Collection<SimplePropertyDefinition> nestedConfigurations = dropChildren(properties
                .stream()
                .filter(this::isConfiguration)
                .filter(it -> !it.getName().equals(it.getPath()) /* root is the one we convert */)
                .map(ref -> new SimplePropertyDefinition(ref.getPath(), ref.getName(), ref.getDisplayName(), "STRING",
                        null, new PropertyValidation(true, null, null, null, null, null, null, null, null, emptySet()),
                        new HashMap<String, String>(ref.getMetadata()) {

                            {
                                put("proxy::type", "reference");
                            }
                        }, null, emptyMap()))
                .collect(toList()));

        final CompletionStage<Void> fillMeta;
        if (!nestedConfigurations.isEmpty()) {
            fillMeta = configurations
                    .getAllConfigurations(lang, placeholders)
                    .thenApply(nodes -> nestedConfigurations.stream().map(it -> {
                        final String type = it.getMetadata().getOrDefault("configurationtype::type", "");
                        final String name = it.getMetadata().getOrDefault("configurationtype::name", "default");
                        return nodes
                                .getNodes()
                                .values()
                                .stream()
                                .filter(node -> type.equals(node.getConfigurationType()) && name.equals(node.getName()))
                                .findFirst()
                                .map(node -> findPotentialIdsAndNames(node.getId()))
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
                        nestedConfigurations.stream())
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

    private CompletionStage<Map<String, String>> findPotentialIdsAndNames(final String formId) {
        return findByFormIdEvent.fireAsync(new OnFindByFormId(formId)).thenCompose(
                e -> ofNullable(e.getResult()).orElseGet(() -> completedFuture(emptyMap())));
    }

    public CompletionStage<Map<String, String>> replaceReferences(final RequestContext context,
            final List<SimplePropertyDefinition> properties, final Map<String, String> instance) {
        if (instance == null) {
            return completedFuture(emptyMap());
        }
        final List<SimplePropertyDefinition> references = properties
                .stream()
                .filter(it -> "reference".equals(it.getMetadata().getOrDefault("proxy::type", "")))
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
        return allOf(
                references
                        .stream()
                        .map(it -> onFindByIdEvent
                                .fireAsync(new OnFindById(context, instance.get(it.getPath())))
                                .thenCompose(byId -> byId.getProperties().thenCompose(props -> byId
                                        .getFormId()
                                        .thenCompose(formId -> configurations.getDetails(context.language(), formId,
                                                context::findPlaceholder))
                                        .thenApply(detail -> replaceReferences(context, detail.getProperties(), props)
                                                .thenApply(nestedProps -> {
                                                    final String root = detail
                                                            .getProperties()
                                                            .stream()
                                                            .map(SimplePropertyDefinition::getPath)
                                                            .sorted()
                                                            .iterator()
                                                            .next();
                                                    config.putAll(nestedProps.entrySet().stream().collect(toMap(
                                                            e -> it.getPath() + e.getKey().substring(root.length()),
                                                            Map.Entry::getValue)));
                                                    return null;
                                                })))))
                        .toArray(CompletableFuture[]::new)).thenApply(ignored -> config);
    }
}
