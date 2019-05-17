/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.server.stitch;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentId;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

// todo: i18n
// see https://www.stitchdata.com/docs/stitch-connect/api
@ApplicationScoped
public class StitchMapper {

    private static final PropertyValidation NO_VALIDATION = new PropertyValidation();

    private static final SimplePropertyDefinition ROOT = new SimplePropertyDefinition("configuration", "configuration",
            "", "OBJECT", null, new PropertyValidation(), emptyMap(), null, new LinkedHashMap<>());

    public ComponentDetail mapSource(final StitchClient.Steps steps, final ConfigTypeNode dataset) {
        final String defaultDisplayName = extractConnectorName(steps);
        final Collection<String> alreadyHandled = dataset
                .getProperties()
                .stream()
                .filter(it -> it.getMetadata().containsKey("stitch::form"))
                .map(it -> it.getMetadata().get("stitch::form"))
                .collect(toSet());
        final Stream<SimplePropertyDefinition> datasetProps = Stream
                .concat(Stream
                        .of(new SimplePropertyDefinition(ROOT.getPath(), ROOT.getName(), ROOT.getDisplayName(),
                                ROOT.getType(), ROOT.getDefaultValue(), ROOT.getValidation(),
                                singletonMap("stitch::component", steps.getType()), ROOT.getPlaceholder(),
                                ROOT.getProposalDisplayNames()),
                                new SimplePropertyDefinition("configuration.dataset", "dataset",
                                        dataset.getProperties().isEmpty() ? "" : dataset.getDisplayName(), "OBJECT",
                                        null, new PropertyValidation(), emptyMap(), null, new LinkedHashMap<>())),
                        dataset
                                .getProperties()
                                .stream()
                                .map(it -> new SimplePropertyDefinition("configuration.dataset." + it.getPath(),
                                        it.getName(), it.getDisplayName(), it.getType(), it.getDefaultValue(),
                                        it.getValidation(), it.getMetadata(), it.getPlaceholder(),
                                        it.getProposalDisplayNames())));
        return new ComponentDetail(
                new ComponentId(generateId(steps.getType(), "source", getFamilyId(steps)), "stitch", "stitch", null,
                        "Stitch", steps.getType()),
                "Stitch " + humanCase(defaultDisplayName), null, "input", 1,
                Stream
                        .concat(datasetProps,
                                steps
                                        .getSteps()
                                        .stream()
                                        .filter(it -> !alreadyHandled.contains(it.getType()))
                                        .flatMap(this::mapProperties))
                        .collect(toList()),
                emptyList(), emptyList(), singletonList("__default__"), emptyList(), emptyMap());
    }

    public ConfigTypeNode mapFamily(final StitchClient.Steps step) {
        final String name = extractConnectorName(step);
        final ConfigTypeNode node = new ConfigTypeNode();
        node.setVersion(1);
        node.setId(getFamilyId(step));
        node.setDisplayName(humanCase(name));
        node.setName(name + "_family");
        node.setEdges(singleton(generateId(step.getType(), "datastore", node.getId())));
        node.setProperties(emptyList());
        node.setActions(emptyList());
        return node;
    }

    private String getFamilyId(final StitchClient.Steps step) {
        return generateId(step.getType(), "family", null);
    }

    public ConfigTypeNode mapDataStore(final StitchClient.Steps steps, final ConfigTypeNode parent) {
        final String familyId = getFamilyId(steps);
        final String name = extractConnectorName(steps);
        final ConfigTypeNode node = new ConfigTypeNode();
        node.setVersion(1);
        node.setId(generateId(steps.getType(), "datastore", familyId));
        node.setParentId(parent.getId());
        node.setConfigurationType("datastore");
        node.setDisplayName(humanCase(name) + " Connection");
        node.setName(name + "_datastore");
        node.setEdges(singleton(generateId(steps.getType(), "dataset", familyId)));
        node
                .setProperties(
                        Stream
                                .concat(mapProperties(ofNullable(findStep(steps, "oauth"))
                                        .orElseGet(() -> findStep(steps, "profile"))), createStitchProperties())
                                .collect(toList()));
        node.setActions(emptyList());
        return node;
    }

    private Stream<SimplePropertyDefinition> createStitchProperties() {
        return Stream
                .of(new SimplePropertyDefinition("stitchConnection", "stitchConnection", "Stitch Connection", "OBJECT",
                        null, NO_VALIDATION, emptyMap(), null, new LinkedHashMap<>()),
                        new SimplePropertyDefinition("stitchConnection.url", "url", "URL", "STRING",
                                "https://api.stitchdata.com/v4/",
                                new PropertyValidation(true, null, null, 1, null, null, null, null, null, emptyList()),
                                emptyMap(), null, new LinkedHashMap<>()),
                        new SimplePropertyDefinition("stitchConnection.token", "token", "OAuth2 Token", "STRING", null,
                                new PropertyValidation(true, null, null, 1, null, null, null, null, null, emptyList()),
                                singletonMap("ui::credential", "true"), null, new LinkedHashMap<>()));
    }

    public ConfigTypeNode mapDataSet(final StitchClient.Steps steps, final ConfigTypeNode parent) {
        final String name = extractConnectorName(steps);
        final ConfigTypeNode node = new ConfigTypeNode();
        node.setVersion(1);
        node.setId(generateId(steps.getType(), "dataset", getFamilyId(steps)));
        node.setParentId(parent.getId());
        node.setDisplayName(humanCase(name));
        node.setName(name + "_dataset");
        node.setConfigurationType("dataset");
        final StitchClient.Step formStep = findStep(steps, "form");
        final Stream<SimplePropertyDefinition> datastoreProps = Stream
                .concat(Stream
                        .of(new SimplePropertyDefinition(ROOT.getPath(), ROOT.getName(), ROOT.getDisplayName(),
                                ROOT.getType(), ROOT.getDefaultValue(), ROOT.getValidation(),
                                singletonMap("stitch::form", formStep.getType()), ROOT.getPlaceholder(),
                                ROOT.getProposalDisplayNames()),
                                new SimplePropertyDefinition("configuration.datastore", "datastore",
                                        parent.getProperties().isEmpty() ? "" : parent.getDisplayName(), "OBJECT", null,
                                        new PropertyValidation(), emptyMap(), null, new LinkedHashMap<>())),
                        parent
                                .getProperties()
                                .stream()
                                .map(it -> new SimplePropertyDefinition("configuration.datastore." + it.getPath(),
                                        it.getName(), it.getDisplayName(), it.getType(), it.getDefaultValue(),
                                        it.getValidation(), it.getMetadata(), it.getPlaceholder(),
                                        it.getProposalDisplayNames())));
        final Stream<? extends SimplePropertyDefinition> formProps =
                mapProperties(formStep).filter(it -> !ROOT.getPath().equals(it.getPath()) /* already handled */);
        final Stream<? extends SimplePropertyDefinition> datafields = createDataFields(steps);
        node.setProperties(Stream.concat(Stream.concat(datastoreProps, formProps), datafields).collect(toList()));
        node.setActions(emptyList());
        node.setEdges(emptySet());
        return node;
    }

    private Stream<SimplePropertyDefinition> createDataFields(final StitchClient.Steps steps) {
        final Collection<SimplePropertyDefinition> fields = new ArrayList<>(4);
        final boolean hasDiscoverSchema =
                steps.getSteps().stream().anyMatch(s -> "discover_schema".equals(s.getType()));
        if (hasDiscoverSchema) {
            final Map<String, String> suggestions = new HashMap<>();
            suggestions.put("action::suggestions", "schema_" + steps.getType());
            suggestions.put("action::suggestions::parameters", "step_form");
            fields
                    .add(new SimplePropertyDefinition("configuration.schema", "schema", "Schema", "ARRAY", null,
                            NO_VALIDATION, suggestions, null, new LinkedHashMap<>()));
            fields
                    .add(new SimplePropertyDefinition("configuration.schema[]", "schema[]", "Schema", "STRING", null,
                            NO_VALIDATION, emptyMap(), null, new LinkedHashMap<>()));
        }
        if (steps.getSteps().stream().anyMatch(s -> "field_selection".equals(s.getType()))) {
            final Map<String, String> suggestions = new HashMap<>();
            suggestions.put("action::suggestions", "fields_" + steps.getType());
            suggestions.put("action::suggestions::parameters", "step_form" + (hasDiscoverSchema ? ",schema" : ""));
            fields
                    .add(new SimplePropertyDefinition("configuration.fields", "fields", "Fields", "ARRAY", null,
                            NO_VALIDATION, suggestions, null, new LinkedHashMap<>()));
            fields
                    .add(new SimplePropertyDefinition("configuration.fields[]", "fields[]", "Fields", "STRING", null,
                            NO_VALIDATION, emptyMap(), null, new LinkedHashMap<>()));
        }
        return fields.stream();
    }

    // https://www.stitchdata.com/docs/stitch-connect/api#connection-step-object
    private StitchClient.Step findStep(final StitchClient.Steps steps, final String name) {
        return steps.getSteps().stream().filter(it -> name.equalsIgnoreCase(it.getType())).findFirst().orElse(null);
    }

    private String extractConnectorName(final StitchClient.Steps steps) {
        return steps.getType().substring(steps.getType().lastIndexOf('.') + 1);
    }

    // https://www.stitchdata.com/docs/stitch-connect/api#connection-step-object
    // this first impl just concatenate all steps, in a later impl we could use tabs
    private Stream<? extends SimplePropertyDefinition> mapProperties(final StitchClient.Step step) {
        if (step == null || step.getProperties() == null || step.getProperties().isEmpty() /* no need to show it */) {
            return Stream.empty();
        }
        final String rootContainer = "configuration.step_" + step.getType().replace('.', '_');
        final String stepName = step.getType().replace("form", "");
        return Stream
                .concat(createRootProperties(rootContainer, stepName, step.getType()),
                        step.getProperties().stream().map(prop -> {
                            final String type = findType(prop);
                            final String displayName = humanCase(prop.getName());
                            return new SimplePropertyDefinition(rootContainer + '.' + prop.getName(), prop.getName(),
                                    displayName, type, null, mapValidation(prop), mapMetadata(prop),
                                    displayName + "...", new LinkedHashMap<>());
                        }));
    }

    private Stream<SimplePropertyDefinition> createRootProperties(final String rootContainer, final String stepName,
            final String formType) {
        return Stream
                .of(new SimplePropertyDefinition(ROOT.getPath(), ROOT.getName(), ROOT.getDisplayName(), ROOT.getType(),
                        ROOT.getDefaultValue(), ROOT.getValidation(), singletonMap("stitch::form", formType),
                        ROOT.getPlaceholder(), ROOT.getProposalDisplayNames()),
                        new SimplePropertyDefinition(rootContainer,
                                rootContainer.substring(rootContainer.lastIndexOf('.') + 1),
                                stepName.isEmpty() ? "Configuration" : humanCase(stepName), "OBJECT", null,
                                new PropertyValidation(), emptyMap(), null, new LinkedHashMap<>()));
    }

    private String humanCase(final String name) {
        switch (name) { // enable to handle particular cases easily
        case "dbname":
            return "DB Name";
        default:
            final char[] array = name.toCharArray();
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (i == 0) {
                    builder.append(Character.toUpperCase(array[i]));
                } else if (array[i] == '_' || array[i] == '.' || array[i] == '-') { // snake style/dotted style
                    builder.append(' ');
                    i++;
                    if (i < array.length) {
                        builder.append(Character.toUpperCase(array[i]));
                    }
                } else if (Character.isUpperCase(array[i])) { // camel style
                    builder.append(' ').append(array[i]);
                } else {
                    builder.append(array[i]);
                }
            }
            return builder.toString().replace(" api$", " API");
        }
    }

    private Map<String, String> mapMetadata(final StitchClient.Property prop) {
        if (prop.isCredential()) {
            return singletonMap("ui::credential", "true");
        }
        return emptyMap();
    }

    private PropertyValidation mapValidation(final StitchClient.Property prop) {
        if (prop.getJsonSchema() == null && !prop.isRequired()) {
            return NO_VALIDATION;
        }
        return new PropertyValidation(prop.isRequired(), null, null, null, null, null, null, null,
                findPattern(prop.getJsonSchema()), emptyList());
    }

    private String findPattern(final StitchClient.JsonSchema jsonSchema) {
        if (jsonSchema == null) {
            return null;
        }
        if (jsonSchema.getPattern() != null) {
            return jsonSchema.getPattern();
        }
        if (jsonSchema.getFormat() != null) {
            return mapFormat(jsonSchema.getFormat());
        }
        if (jsonSchema.getAnyOf() != null && !jsonSchema.getAnyOf().isEmpty()) {
            final String newPattern = jsonSchema
                    .getAnyOf()
                    .stream()
                    .map(this::findPattern)
                    .filter(Objects::nonNull)
                    .collect(joining(")|(", "(", ")"));
            return newPattern.length() <= 2 /* () */ ? null : newPattern;
        }
        return null;
    }

    private String mapFormat(final String format) {
        switch (format) {
        case "date-time":
            return "^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2})\\:(\\d{2})\\:(\\d{2})[+-](\\d{2})\\:(\\d{2})";
        case "uri":
            return "^\\w+:(\\/?\\/?)[^\\s]+$";
        case "ipv4":
            return "^([0-9]{1,3}\\.){3}[0-9]{1,3}$";
        case "ipv6":
            return "^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|"
                    + "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|"
                    + "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|"
                    + "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|"
                    + "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|"
                    + "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" + ":((:[0-9a-fA-F]{1,4}){1,7}|:)|"
                    + "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|" + "::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|"
                    + "(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|"
                    + "1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|"
                    + "(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$";
        case "hostname":
            return "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*"
                    + "([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        default:
            // unsupported
            return null;
        }
    }

    private String findType(final StitchClient.Property prop) {
        if (prop.getJsonSchema() != null && prop.getJsonSchema().getType() != null) {
            if ("integer".equals(prop.getJsonSchema().getType().toLowerCase(Locale.ROOT))) { // other !string types not
                                                                                             // used
                return "NUMBER";
            }
        }
        return "STRING";
    }

    // whatever is fine while unique for the whole component database and stable,
    // note: WebAppComponentProxy relies on this pattern
    public String generateId(final String type, final String marker, final String family) {
        return Base64
                .getUrlEncoder()
                .encodeToString(("extension::stitch::" + marker + "::" + type
                        + ofNullable(family).map(it -> "::" + it).orElse("")).getBytes(StandardCharsets.UTF_8));
    }
}
