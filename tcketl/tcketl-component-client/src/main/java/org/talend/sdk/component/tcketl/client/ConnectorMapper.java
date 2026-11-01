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
package org.talend.sdk.component.tcketl.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorDefinition;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorSummary;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

/**
 * The anti-corruption layer: turns what the server sent into domain types, so that no wire convention (metadata keys,
 * type names, the sorting of properties) leaks out of this module.
 */
final class ConnectorMapper {

    private static final System.Logger LOGGER = System.getLogger(ConnectorMapper.class.getName());

    private static final String INFINITE = "mapper::infinite";

    private static final String OPTIONAL_ROW = "mapper::optionalRow";

    private static final String TOOLTIP = "documentation::tooltip";

    private ConnectorMapper() {
        // static helpers only
    }

    /** Empty when the entry cannot even be identified. */
    static Optional<ConnectorSummary> summary(final Wire.ComponentIndex index) {
        if (index == null) {
            return Optional.empty();
        }
        return ref(index.id(), index.version()).map(ref -> {
            final Map<String, String> metadata = clean(index.metadata());
            return new ConnectorSummary(ref, index.id().familyId(), index.familyDisplayName(), index.displayName(),
                    strings(index.categories()), provisionalRole(index.type()),
                    MetadataParser.isTrue(metadata, INFINITE),
                    MetadataParser.text(metadata, MetadataParser.DOCUMENTATION));
        });
    }

    /** @throws IllegalArgumentException when the connector cannot be identified */
    static ConnectorDefinition definition(final Wire.ComponentDetail detail) {
        final ConnectorRef ref = ref(detail.id(), detail.version())
                .orElseThrow(() -> new IllegalArgumentException("the component identity is incomplete"));
        final Map<String, String> metadata = clean(detail.metadata());
        final List<ActionDefinition> actions = new ArrayList<>();
        if (detail.actions() != null) {
            for (final Wire.ActionReference action : detail.actions()) {
                action(action).ifPresent(actions::add);
            }
        }
        return new ConnectorDefinition(ref, detail.displayName(), strings(detail.inputFlows()),
                strings(detail.outputFlows()), MetadataParser.isTrue(metadata, INFINITE),
                MetadataParser.isTrue(metadata, OPTIONAL_ROW),
                MetadataParser.text(metadata, MetadataParser.DOCUMENTATION), properties(detail.properties()), actions);
    }

    /** The type of the index entry is only a hint: the flows of the details give the real role. */
    static ConnectorRole provisionalRole(final String type) {
        if ("input".equalsIgnoreCase(type)) {
            return ConnectorRole.INPUT;
        }
        return "standalone".equalsIgnoreCase(type) ? ConnectorRole.STANDALONE : ConnectorRole.PROCESSOR;
    }

    /** A property that cannot be addressed (no path) is dropped, it must not make the whole connector unusable. */
    static List<PropertyDefinition> properties(final List<Wire.Property> properties) {
        final List<PropertyDefinition> mapped = new ArrayList<>();
        if (properties != null) {
            for (final Wire.Property property : properties) {
                if (property == null || isBlank(property.path())) {
                    LOGGER.log(System.Logger.Level.WARNING, "Ignoring a property without path");
                } else {
                    mapped.add(property(property));
                }
            }
        }
        return mapped;
    }

    static PropertyDefinition property(final Wire.Property wire) {
        final Map<String, String> metadata = clean(wire.metadata());
        final PropertyDefinition.Builder property = PropertyDefinition
                .builder(wire.path(), type(wire.type()))
                .name(wire.name() == null || wire.name().isBlank() ? null : wire.name())
                .displayName(wire.displayName())
                .defaultValue(MetadataParser.defaultValue(metadata, wire.defaultValue()))
                .placeholder(wire.placeholder())
                .documentation(MetadataParser.text(metadata, MetadataParser.DOCUMENTATION))
                .tooltip(MetadataParser.isTrue(metadata, TOOLTIP))
                .proposalLabels(clean(wire.proposalDisplayNames()))
                .constraints(constraints(wire.validation()))
                .ui(MetadataParser.ui(metadata))
                .layout(MetadataParser.layout(metadata))
                .visibility(MetadataParser.visibility(metadata))
                .configurationType(MetadataParser.configurationType(metadata))
                .parameterIndex(MetadataParser.parameterIndex(metadata))
                .metadata(metadata);
        MetadataParser.actions(metadata).forEach(property::action);
        return property.build();
    }

    static Constraints constraints(final Wire.Validation validation) {
        if (validation == null) {
            return Constraints.NONE;
        }
        Constraints constraints = Constraints.NONE
                .withRequired(Boolean.TRUE.equals(validation.required()))
                .withUniqueItems(Boolean.TRUE.equals(validation.uniqueItems()));
        if (validation.min() != null) {
            constraints = constraints.withMin(validation.min());
        }
        if (validation.max() != null) {
            constraints = constraints.withMax(validation.max());
        }
        if (validation.minLength() != null) {
            constraints = constraints.withMinLength(validation.minLength());
        }
        if (validation.maxLength() != null) {
            constraints = constraints.withMaxLength(validation.maxLength());
        }
        if (validation.minItems() != null) {
            constraints = constraints.withMinItems(validation.minItems());
        }
        if (validation.maxItems() != null) {
            constraints = constraints.withMaxItems(validation.maxItems());
        }
        if (validation.pattern() != null && !validation.pattern().isBlank()) {
            constraints = constraints.withPattern(validation.pattern());
        }
        return constraints.withEnumValues(strings(validation.enumValues()));
    }

    private static Optional<ActionDefinition> action(final Wire.ActionReference action) {
        if (action == null || isBlank(action.type()) || isBlank(action.name())) {
            LOGGER.log(System.Logger.Level.WARNING, "Ignoring an action reference without type or name");
            return Optional.empty();
        }
        return Optional
                .of(new ActionDefinition(action.family(), action.type(), action.name(), action.displayName(),
                        properties(action.properties())));
    }

    private static Optional<ConnectorRef> ref(final Wire.ComponentId id, final int version) {
        if (id == null || isBlank(id.id()) || isBlank(id.family()) || isBlank(id.name())) {
            return Optional.empty();
        }
        return Optional.of(
                new ConnectorRef(new ConnectorId(id.id()), id.family(), id.name(), id.plugin(), Math.max(version, 0)));
    }

    /** Unknown names are rendered as text, so that a type added by a newer server stays editable. */
    private static PropertyType type(final String wire) {
        if (wire != null) {
            for (final PropertyType type : PropertyType.values()) {
                if (type.name().equalsIgnoreCase(wire.trim())) {
                    return type;
                }
            }
        }
        return PropertyType.STRING;
    }

    /** A copy in the server order without the entries the domain refuses (null keys or values). */
    private static Map<String, String> clean(final Map<String, String> source) {
        final Map<String, String> cleaned = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    cleaned.put(key, value);
                }
            });
        }
        return cleaned;
    }

    private static List<String> strings(final List<String> source) {
        final List<String> strings = new ArrayList<>();
        if (source != null) {
            source.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).forEach(strings::add);
        }
        return strings;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
