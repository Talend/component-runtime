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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.stream.JsonCollectors.toJsonObject;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;

import org.eclipse.microprofile.config.Config;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.proxy.api.integration.application.ReferenceService;
import org.talend.sdk.component.proxy.api.integration.application.Values;
import org.talend.sdk.component.proxy.api.service.ConfigurationFormatter;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.proxy.jcache.CacheResolverManager;
import org.talend.sdk.component.proxy.jcache.ProxyCacheKeyGenerator;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.service.client.ComponentClient;
import org.talend.sdk.component.proxy.service.client.ConfigurationClient;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.service.lang.Substitutor;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ActionService {

    private final ConfigTypeNode datastoreNode = new ConfigTypeNode("datastore", 0, null, "datastore", "datastore",
            "datastore", emptySet(), new ArrayList<>(), new ArrayList<>());

    private final ConfigTypeNode noFamily = new ConfigTypeNode();

    @Inject
    private ErrorProcessor errorProcessor;

    @Inject
    @UiSpecProxy
    private UiSpecService<UiSpecContext> uiSpecService;

    @Inject
    private ComponentClient componentClient;

    @Inject
    private ConfigurationClient configurationClient;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private JsonMapService jsonMapService;

    @Inject
    @UiSpecProxy
    private Client<UiSpecContext> client;

    @Inject
    private ModelEnricherService modelEnricherService;

    @Inject // to have cache activated and not handle it manually
    private ActionService self;

    @Inject
    @UiSpecProxy
    private javax.ws.rs.client.Client http;

    @Inject
    @UiSpecProxy
    private Config config;

    @Inject
    private Substitutor substitutor;

    @Inject
    private ReferenceService referenceService;

    @Inject
    private ConfigurationFormatter formatter;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    @UiSpecProxy
    private JsonBuilderFactory builderFactory;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Inject
    private I18n i18n;

    private final GenericType<List<Map<String, Object>>> listType = new GenericType<List<Map<String, Object>>>() {

    };

    public CompletionStage<Map<String, Object>> createStage(final String family, final String type, final String action,
            final UiSpecContext context, final Map<String, Object> params) {
        // family is ignored since we virtually add it for all families (=local exec)
        if (isBuiltin(action)) {
            return findBuiltInAction(action, context, params);
        }
        if ("dynamic_values".equals(type)) {
            return self.findProposable(family, type, action, context);
        }
        final Map<String, Object> newProps = new HashMap<>(params);
        final CompletableFuture<?>[] referenceResolutions =
                params.entrySet().stream().filter(it -> it.getKey().endsWith(".$selfReference")).map(it -> {
                    final String configType =
                            ofNullable(params.get(it.getKey() + "Type")).map(String::valueOf).orElse(null);
                    return referenceService
                            .findPropertiesById(configType, String.valueOf(it.getValue()), context)
                            .thenCompose(form -> configurationClient
                                    .getDetails(context.getLanguage(), form.getFormId(),
                                            context.getPlaceholderProvider())
                                    .thenApply(node -> {
                                        final String propPrefix = node
                                                .getProperties()
                                                .stream()
                                                .filter(p -> p.getName().equals(p.getPath()))
                                                .findFirst()
                                                .orElseThrow(() -> new IllegalStateException(
                                                        "Can't find configuration for id :" + form.getFormId()))
                                                .getName();
                                        final String actionPrefix = it.getKey().replace(".$selfReference", "");
                                        synchronized (newProps) {
                                            ofNullable(form.getProperties())
                                                    .ifPresent(props -> props
                                                            .entrySet()
                                                            .stream()
                                                            .filter(k -> k.getKey().startsWith(propPrefix))
                                                            .forEach(k -> newProps
                                                                    .put(actionPrefix
                                                                            + k.getKey().substring(propPrefix.length()),
                                                                            k.getValue())));
                                        }
                                        return form;
                                    }));
                }).toArray(CompletableFuture[]::new);
        return CompletableFuture
                .allOf(referenceResolutions)
                .thenCompose(ignored -> client.action(family, type, action, context.getLanguage(), newProps, context));
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.actions.proposables",
            cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
    public CompletionStage<Map<String, Object>> findProposable(final String family, final String type,
            final String action, final UiSpecContext context) {
        // we recreate the context and don't pass it as a param to ensure the cache key is right
        return client.action(family, type, action, context.getLanguage(), emptyMap(), context);
    }

    public boolean isBuiltin(final String action) {
        return action != null && action.startsWith("builtin::");
    }

    // IMPORTANT: ensure to register the action in
    // org.talend.sdk.component.proxy.service.ModelEnricherService.BUILTIN_ACTIONS
    public CompletionStage<Map<String, Object>> findBuiltInAction(final String action, final UiSpecContext ctx,
            final Map<String, Object> params) {
        switch (action) {
        case "builtin::roots":
            return findRoots(ctx.getLanguage(), ctx.getPlaceholderProvider());
        case "builtin::root::reloadFromId":
            return ofNullable(params.get("id"))
                    .map(id -> createNewFormFromId(String.valueOf(id), ctx))
                    .orElseGet(() -> CompletableFuture.completedFuture(emptyMap()));
        case "builtin::root::reloadFromParentEntityId":
            return ofNullable(params.get("id"))
                    .map(id -> createNewFormFromParentEntityId(String.valueOf(params.get("type")), String.valueOf(id),
                            ctx, null))
                    .orElseGet(() -> CompletableFuture.completedFuture(emptyMap()));
        default:
            if (action.startsWith("builtin::http::dynamic_values(")) {
                return http(ctx.getPlaceholderProvider(), csvToParams(action, "builtin::http::dynamic_values("));
            } else if (action.startsWith("builtin::references(")) {
                return references(ctx, csvToParams(action, "builtin::references("));
            } else if (action.startsWith("builtin::root::reloadFromParentEntityIdAndType(")) {
                final Map<String, Object> enforcedParams =
                        csvToParams(action, "builtin::root::reloadFromParentEntityIdAndType(");
                return createNewFormFromParentEntityId(
                        ofNullable(enforcedParams.get("type"))
                                .map(String::valueOf)
                                .orElseThrow(() -> new IllegalArgumentException("type parameter not provided")),
                        ofNullable(enforcedParams.get("parentId"))
                                .map(String::valueOf)
                                .orElseThrow(() -> new IllegalArgumentException("parentId parameter not provided")),
                        ctx,
                        ofNullable(params.get("$datasetMetadata.childrenType"))
                                .map(String::valueOf)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "$datasetMetadata.childrenType parameter not provided"))).thenApply(form -> {
                                            /*
                                             * fixme: when a specific trigger is available to handle reload of only part
                                             * of a form.
                                             * for now we keep the $datasetMetadata on reload
                                             */
                                            Map.class
                                                    .cast(Map.class
                                                            .cast(form.get("properties"))
                                                            .computeIfAbsent("$datasetMetadata",
                                                                    k -> new HashMap<String, Object>()))
                                                    .putAll(params
                                                            .entrySet()
                                                            .stream()
                                                            .collect(toMap(e -> e
                                                                    .getKey()
                                                                    .substring(17 /* "$datasetMetadat.".length()+1 */),
                                                                    Map.Entry::getValue)));
                                            return form;
                                        });
            }
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private CompletionStage<Map<String, Object>> references(final UiSpecContext context,
            final Map<String, Object> params) {
        // family is not yet used, required changes in the SPI, to do when the module moves
        final String family = ofNullable(params.get("family")).map(String::valueOf).orElse(null);
        if (family == null) {
            log.error("No family sent to builtin::references trigger, this will likely break very soon");
        }
        final String type = String.class.cast(requireNonNull(params.get("type"), "reference type must not be null"));
        final String name = String.class.cast(requireNonNull(params.get("name"), "reference name must not be null"));
        // todo: add family in this spi and likely make {family, type, name} an object named "ConfigurationTypeKey"
        return referenceService
                .findReferencesByTypeAndName(family, type, name, context)
                .thenApply(jsonMapService::toJsonMap);
    }

    private Map<String, Object> csvToParams(final String value, final String prefix) {
        return Stream
                .of(value.substring(prefix.length(), value.length() - 1).split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .map(it -> it.split("="))
                .collect(toMap(it -> it[0], it -> it[1]));
    }

    // todo: cache most of that computation to do it only once, not critical for now (must use placeholders in the key)
    // @CacheResult -> we miss an eviction rule to do that ATM
    private CompletionStage<Map<String, Object>> http(final Function<String, String> placeholderProvider,
            final Map<String, Object> params) {
        final String url = substitutor
                .compile(requireNonNull(String.class.cast(params.get("url")), "No url specificed for a http trigger"))
                .apply(placeholderProvider);
        final List<String> headers = Stream
                .of(String.class.cast(params.getOrDefault("headers", "")).split(";"))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .collect(toList());
        Invocation.Builder request = http
                .target(config.getOptionalValue(url, String.class).orElse(url))
                .request(String.class.cast(params.getOrDefault("accept", APPLICATION_JSON)));
        for (final String header : headers) {
            final String headerValue = placeholderProvider.apply(header);
            if (headerValue == null) {
                continue;
            }
            request = request.header(header, headerValue);
        }

        final CompletionStageRxInvoker rx = request.rx();

        final CompletionStage<List<Map<String, Object>>> list;
        if (!Boolean.parseBoolean(String.valueOf(params.getOrDefault("object", "false")))) {
            list = rx.get(listType);
        } else {
            list = rx
                    .get(Object.class)
                    .thenApply(object -> List.class
                            .cast(Map.class.cast(object).get(params.getOrDefault("objectKey", "items"))));
        }

        final String idName = String.class.cast(params.getOrDefault("id", "id"));
        final String labelName = String.class.cast(params.getOrDefault("name", "name"));
        return list
                .thenApply(it -> ofNullable(it)
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .filter(map -> map.containsKey(idName) || map.containsKey(labelName))
                        .map(map -> new Values.Item(String.class.cast(map.getOrDefault(idName, map.get(labelName))),
                                String.class.cast(map.getOrDefault(labelName, map.get(idName)))))
                        .collect(toList()))
                .thenApply(Values::new)
                .thenApply(jsonMapService::toJsonMap);
    }

    private CompletableFuture<Map<String, Object>> createNewFormFromParentEntityId(final String configType,
            final String entityId, final UiSpecContext context, final String selectedId) {
        return referenceService
                .findPropertiesById(configType, entityId, context)
                .thenCompose(form -> configurationClient
                        .getAllConfigurations(context.getLanguage(), context.getPlaceholderProvider())
                        .thenCompose(nodes -> {
                            final List<ConfigTypeNode> childrenSpec =
                                    configurationService.findChildren(form.getFormId(), nodes);
                            final ConfigTypeNode parentSpec = nodes.getNodes().get(form.getFormId());
                            switch (childrenSpec.size()) {
                            case 0:
                                throw new IllegalArgumentException(
                                        "No dataset available for connection " + parentSpec.getDisplayName());
                            case 1:
                                return configurationClient
                                        .getDetails(context.getLanguage(), parentSpec.getId(),
                                                context.getPlaceholderProvider())
                                        .thenCompose(
                                                parent -> configurationClient
                                                        .getDetails(context.getLanguage(),
                                                                childrenSpec.iterator().next().getId(),
                                                                context.getPlaceholderProvider())
                                                        .thenCompose(
                                                                child -> toNewForm(
                                                                        configurationService
                                                                                .getFamilyOf(child.getId(), nodes)
                                                                                .getName(),
                                                                        context, child, entityId, parent)));
                            default: // add a dropdown to select the dataset
                                return createChildrenChooserForm(context, childrenSpec,
                                        nodes.getNodes().get(form.getFormId()), entityId, selectedId, nodes);
                            }
                        }))
                .thenApply(jsonMapService::toJsonMap)
                .toCompletableFuture();
    }

    private CompletionStage<NewForm> createChildrenChooserForm(final UiSpecContext context,
            final List<ConfigTypeNode> childrenSpec, final ConfigTypeNode parentSpec, final String parentEntityId,
            final String selectedId, final ConfigTypeNodes nodes) {

        final ConfigTypeNode refChild = childrenSpec
                .stream()
                .filter(it -> it.getId().equals(selectedId))
                .findFirst()
                .orElseGet(() -> childrenSpec.iterator().next());
        final ConfigTypeNode family = configurationService.getFamilyOf(refChild.getId(), nodes);
        final ConfigTypeNode formModel = modelEnricherService.enrich(refChild, context.getLanguage());
        formModel
                .getProperties()
                .add(createChildrenChooserProperty(childrenSpec, parentEntityId, context.getLanguage(), selectedId,
                        modelEnricherService.findPropertyPrefixForEnrichingProperty(refChild.getConfigurationType())));
        return componentClient
                .getAllComponents(context.getLanguage(), context.getPlaceholderProvider())
                .thenCompose(components -> uiSpecService
                        .convert(family.getName(), context.getLanguage(), formModel, context)
                        .thenApply(ui -> new UiNode(ui,
                                new Node(selectedId, refChild.getDisplayName(), family.getId(), family.getDisplayName(),
                                        ofNullable(family.getId())
                                                .map(id -> configurationService.findIcon(id, components))
                                                .orElse(null),
                                        emptyList(), null, refChild.getName()))))
                .thenApply(this::toNewFormResponse)
                .thenCompose(
                        newForm -> handleReferences(configurationService.getFamilyOf(refChild.getId(), nodes).getName(),
                                context, refChild, parentEntityId, parentSpec, newForm));
    }

    private LinkedHashMap<String, String> createConfigTypesValues(final List<ConfigTypeNode> configTypes) {
        return configTypes
                .stream()
                .sorted(comparing(ConfigTypeNode::getName))
                .collect(toMap(ConfigTypeNode::getId, ConfigTypeNode::getDisplayName, (x, y) -> x, LinkedHashMap::new));
    }

    // we assume the children are homogeneous here, this is an assumption acceptable for now but can be wrong later
    private SimplePropertyDefinition createChildrenChooserProperty(final List<ConfigTypeNode> childrenTypes,
            final String parentEntityId, final String locale, final String selectedItem, final String propertyPrefix) {
        final Map<String, String> metadata = new HashMap<>();
        metadata
                .put("action::reloadFromParentEntityIdAndType",
                        "builtin::root::reloadFromParentEntityIdAndType(" + "parentId=" + parentEntityId + ",type="
                                + childrenTypes.iterator().next().getConfigurationType() + ")");
        metadata.put("action::reloadFromParentEntityIdAndType::parameters", "..");
        final String simpleName = "childrenType";
        final String path =
                ofNullable(propertyPrefix).filter(it -> !it.isEmpty()).map(p -> p + '.').orElse("") + simpleName;
        final LinkedHashMap<String, String> values = createConfigTypesValues(childrenTypes);
        return new SimplePropertyDefinition(path, simpleName,
                i18n.actionServiceChildrenTypePropertyDisplayName(new Locale(ofNullable(locale).orElse(""))), "enum",
                selectedItem,
                new PropertyValidation(true, null, null, null, null, null, null, null, null, values.keySet()), metadata,
                null, values);
    }

    private CompletionStage<NewForm> toNewForm(final String family, final UiSpecContext context,
            final ConfigTypeNode node, final String refId, final ConfigTypeNode parentFormSpec) {
        return self
                .getNewForm(context, node.getId())
                .thenCompose(newForm -> handleReferences(family, context, node, refId, parentFormSpec, newForm));
    }

    private CompletionStage<NewForm> handleReferences(final String family, final UiSpecContext context,
            final ConfigTypeNode node, final String refId, final ConfigTypeNode parentFormSpec, final NewForm newForm) {
        final Map<String, String> configInstance = new HashMap<>();
        final SimplePropertyDefinition refProp = node
                .getProperties()
                .stream()
                .filter(it -> it
                        .getMetadata()
                        .getOrDefault("configurationtype::name", "")
                        .equals(parentFormSpec.getName())
                        && it
                                .getMetadata()
                                .getOrDefault("configurationtype::type", "")
                                .equals(parentFormSpec.getConfigurationType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No parent matched for form " + node.getId() + "(entity=" + refId + ")"));

        // assumed not in an array
        configInstance.put(refProp.getPath() + ".$selfReference", refId);
        configInstance.put(refProp.getPath() + ".$selfReferenceType", node.getConfigurationType());

        return propertiesService
                .filterProperties(family, node.getProperties(), context)
                .thenCompose(props -> propertiesService.replaceReferences(context, props, configInstance))
                .thenApply(props -> {
                    if (node.getProperties() != null && !node.getProperties().isEmpty()) {
                        final Map<String, String> mergedWithDefaults =
                                new HashMap<>(formatter.flatten(newForm.getProperties()));
                        mergedWithDefaults.putAll(props);
                        final JsonObject properties = formatter.unflatten(node.getProperties(), mergedWithDefaults);
                        final JsonObject enrichedProperties = newForm
                                .getProperties()
                                .keySet()
                                .stream()
                                .filter(it -> newForm
                                        .getProperties()
                                        .get(it)
                                        .getValueType() == JsonValue.ValueType.OBJECT
                                        && node.getProperties().stream().noneMatch(prop -> prop.getName().equals(it)))
                                .collect(Collector
                                        .of(builderFactory::createObjectBuilder,
                                                (b, k) -> b.add(k, newForm.getProperties().getJsonObject(k)),
                                                JsonObjectBuilder::addAll, JsonObjectBuilder::build));
                        if (!enrichedProperties.isEmpty()) {
                            newForm
                                    .setProperties(Stream
                                            .of(properties, enrichedProperties)
                                            .flatMap(it -> it.entrySet().stream())
                                            .collect(toJsonObject()));
                        } else {
                            newForm.setProperties(properties);
                        }
                    }
                    return addFormId(node.getId(), newForm);
                });
    }

    private NewForm addFormId(final String nodeId, final NewForm newForm) {
        newForm
                .setProperties(ofNullable(newForm.getProperties())
                        .map(builderFactory::createObjectBuilder)
                        .orElseGet(builderFactory::createObjectBuilder)
                        .add("$formId", nodeId)
                        .build());
        return newForm;
    }

    @CacheResult(cacheName = "org.talend.sdk.component.proxy.actions.getnewform",
            cacheResolverFactory = CacheResolverManager.class, cacheKeyGenerator = ProxyCacheKeyGenerator.class)
    public CompletionStage<NewForm> getNewForm(final UiSpecContext context, final String id) {
        return findUiSpec(id, context).thenApply(this::toNewFormResponse);
    }

    private CompletableFuture<Map<String, Object>> createNewFormFromId(final String id, final UiSpecContext context) {
        return self
                .getNewForm(context, id)
                .thenApply(f -> addFormId(id, f))
                .thenApply(jsonMapService::toJsonMap)
                .toCompletableFuture();
    }

    private NewForm toNewFormResponse(final UiNode uiNode) {
        return new NewForm(uiNode.getUi().getJsonSchema(), uiNode.getUi().getUiSchema(),
                jsonb.fromJson(jsonb.toJson(uiNode.getUi().getProperties()), JsonObject.class), uiNode.getMetadata());
    }

    private CompletionStage<UiNode> findUiSpec(final String id, final UiSpecContext context) {
        if (id.isEmpty()) {
            return CompletableFuture
                    .completedFuture(datastoreNode)
                    .thenApply(node -> modelEnricherService.enrich(node, context.getLanguage()))
                    .thenCompose(detail -> toUiNode(context, detail, null, noFamily));
        }
        final CompletionStage<ComponentIndices> allComponents =
                componentClient.getAllComponents(context.getLanguage(), context.getPlaceholderProvider());
        return configurationClient
                .getAllConfigurations(context.getLanguage(), context.getPlaceholderProvider())
                .thenApply(nodes -> configurationService.getFamilyOf(id, nodes))
                .thenCompose(familyNode -> getEnrichedNode(id, context, familyNode.getName())
                        .thenCompose(detail -> allComponents
                                .thenCompose(components -> toUiNode(context, detail, components, familyNode))));
    }

    private CompletionStage<ConfigTypeNode> getEnrichedNode(final String id, final UiSpecContext context,
            final String family) {
        return getNode(id, context.getLanguage(), context.getPlaceholderProvider())
                .thenApply(node -> modelEnricherService.enrich(node, context.getLanguage()))
                .thenCompose(node -> propertiesService
                        .filterProperties(family, node.getProperties(), context)
                        .thenApply(newProps -> {
                            node.setProperties(newProps);
                            return node;
                        }));
    }

    private CompletionStage<UiNode> toUiNode(final UiSpecContext context, final ConfigTypeNode detail,
            final ComponentIndices iconComponents, final ConfigTypeNode family) {
        return toUiSpec(detail, family, context)
                .thenApply(ui -> new UiNode(ui,
                        new Node(detail.getId(), detail.getDisplayName(), family.getId(), family.getDisplayName(),
                                ofNullable(family.getId())
                                        .map(id -> configurationService.findIcon(id, iconComponents))
                                        .orElse(null),
                                detail.getEdges(), detail.getVersion(), detail.getName())));
    }

    private CompletionStage<ConfigTypeNode> getNode(final String id, final String lang,
            final Function<String, String> placeholderProvider) {
        return id.isEmpty()
                // todo: drop that hardcoded datastore string
                ? CompletableFuture
                        .completedFuture(new ConfigTypeNode("datastore", 0, null, "datastore", "datastore", "datastore",
                                emptySet(), new ArrayList<>(), new ArrayList<>()))
                : configurationClient.getDetails(lang, id, placeholderProvider);
    }

    private CompletionStage<Ui> toUiSpec(final ConfigTypeNode detail, final ConfigTypeNode family,
            final UiSpecContext context) {
        return configurationService
                .filterNestedConfigurations(family.getName(), detail, context)
                .thenCompose(newDetail -> uiSpecService
                        .convert(family.getName(), context.getLanguage(), newDetail, context));
    }

    private CompletableFuture<Map<String, Object>> findRoots(final String lang,
            final Function<String, String> placeholderProvider) {
        return configurationClient
                .getAllConfigurations(lang, placeholderProvider)
                .thenApply(configs -> configurationService.getRootConfiguration(configs, ignored -> null))
                .thenApply(configs -> new Values(configs
                        .getNodes()
                        .values()
                        .stream()
                        .map(it -> new Values.Item(it.getId(), it.getLabel()))
                        .sorted(comparing(Values.Item::getLabel))
                        .collect(toList())))
                .thenApply(jsonMapService::toJsonMap)
                .toCompletableFuture();
    }

    private RequestContext newContext(final String lang, final Function<String, String> placeholderProvider) {
        return new RequestContext() {

            @Override
            public String language() {
                return lang;
            }

            @Override
            public String findPlaceholder(final String attributeName) {
                return placeholderProvider.apply(attributeName);
            }

            @Override
            public Object attribute(final String key) {
                return null;
            }
        };
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewForm {

        private JsonSchema jsonSchema;

        private Collection<UiSchema> uiSchema;

        private JsonObject properties;

        private Node metadata;
    }

    @Data
    static class ComparableConfigTypeNode implements Comparable<ComparableConfigTypeNode> {

        private final ConfigTypeNode delegate;

        @Override
        public int compareTo(final ComparableConfigTypeNode o) {
            if (o == null) {
                return -1;
            }
            if (o == this || delegate == o.getDelegate()) {
                return 0;
            }
            if (isNested(this, o)) {
                return -1;
            }
            if (isNested(o, this)) {
                return 1;
            }
            // just to sort globally
            final int myPropCount = delegate.getProperties().size();
            final int otherPropCount = o.getDelegate().getProperties().size();
            if (myPropCount == otherPropCount) {
                return delegate.getId().compareTo(o.getDelegate().getId());
            }
            return myPropCount - otherPropCount;
        }

        private boolean isNested(final ComparableConfigTypeNode o1, final ComparableConfigTypeNode o2) {
            return o1.delegate
                    .getProperties()
                    .stream()
                    .allMatch(it -> o2
                            .getDelegate()
                            .getProperties()
                            .stream()
                            .anyMatch(n -> n.getPath().equals(it.getPath())));
        }
    }
}
