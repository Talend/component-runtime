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
package org.talend.sdk.component.tcketl.domain.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * A configuration property as described by the server, with its metadata already interpreted. {@code path} is the
 * dotted path from the root option, array elements being written {@code list[]}; {@code name} is its last segment.
 * {@code defaultValue} is empty when there is none: for an array or an object it is a JSON text. {@code metadata}
 * keeps the raw server entries, interpreted or not.
 */
public record PropertyDefinition(
        String path,
        String name,
        String displayName,
        PropertyType type,
        String defaultValue,
        String placeholder,
        String documentation,
        boolean tooltip,
        Map<String, String> proposalLabels,
        Constraints constraints,
        UiHints ui,
        LayoutHints layout,
        VisibilityCondition visibility,
        List<ActionBinding> actions,
        ConfigurationTypeRef configurationType,
        int parameterIndex,
        Map<String, String> metadata
) {

    /** Value of {@code parameterIndex} for a property that is not a top-level parameter. */
    public static final int NOT_A_PARAMETER = -1;

    public PropertyDefinition {
        Preconditions.requireNonBlank(path, "path");
        Preconditions.requireNonBlank(name, "name");
        displayName = Preconditions.orDefault(displayName, name);
        Preconditions.requireNonNull(type, "type");
        defaultValue = Preconditions.nullToEmpty(defaultValue);
        placeholder = Preconditions.nullToEmpty(placeholder);
        documentation = Preconditions.nullToEmpty(documentation);
        proposalLabels = Preconditions.orderedCopy(proposalLabels, "proposalLabels");
        Preconditions.requireNonNull(constraints, "constraints");
        Preconditions.requireNonNull(ui, "ui");
        Preconditions.requireNonNull(layout, "layout");
        Preconditions.requireNonNull(visibility, "visibility");
        actions = Preconditions.immutableCopy(actions, "actions");
        Preconditions.requireNonNull(configurationType, "configurationType");
        parameterIndex = Math.max(parameterIndex, NOT_A_PARAMETER);
        metadata = Preconditions.orderedCopy(metadata, "metadata");
    }

    public static Builder builder(final String path, final PropertyType type) {
        return new Builder(path, type);
    }

    public Optional<ActionBinding> action(final String actionType) {
        return actions.stream().filter(action -> action.type().equals(actionType)).findFirst();
    }

    /** True for the properties that are a parameter of the component or of the action they belong to. */
    public boolean isParameter() {
        return parameterIndex != NOT_A_PARAMETER;
    }

    public static final class Builder {

        private final String path;

        private final PropertyType type;

        private String name;

        private String displayName = "";

        private String defaultValue = "";

        private String placeholder = "";

        private String documentation = "";

        private boolean tooltip;

        private Map<String, String> proposalLabels = Map.of();

        private Constraints constraints = Constraints.NONE;

        private UiHints ui = UiHints.NONE;

        private LayoutHints layout = LayoutHints.NONE;

        private VisibilityCondition visibility = VisibilityCondition.ALWAYS;

        private final List<ActionBinding> actions = new ArrayList<>();

        private ConfigurationTypeRef configurationType = ConfigurationTypeRef.NONE;

        private int parameterIndex = NOT_A_PARAMETER;

        private Map<String, String> metadata = Map.of();

        private Builder(final String path, final PropertyType type) {
            this.path = Preconditions.requireNonBlank(path, "path");
            this.type = Preconditions.requireNonNull(type, "type");
        }

        /** Defaults to the last segment of the path. */
        public Builder name(final String value) {
            this.name = value;
            return this;
        }

        public Builder displayName(final String value) {
            this.displayName = value;
            return this;
        }

        public Builder defaultValue(final String value) {
            this.defaultValue = value;
            return this;
        }

        public Builder placeholder(final String value) {
            this.placeholder = value;
            return this;
        }

        public Builder documentation(final String value) {
            this.documentation = value;
            return this;
        }

        public Builder tooltip(final boolean value) {
            this.tooltip = value;
            return this;
        }

        public Builder proposalLabels(final Map<String, String> value) {
            this.proposalLabels = value;
            return this;
        }

        public Builder constraints(final Constraints value) {
            this.constraints = value;
            return this;
        }

        public Builder ui(final UiHints value) {
            this.ui = value;
            return this;
        }

        public Builder layout(final LayoutHints value) {
            this.layout = value;
            return this;
        }

        public Builder visibility(final VisibilityCondition value) {
            this.visibility = value;
            return this;
        }

        public Builder action(final ActionBinding value) {
            this.actions.add(value);
            return this;
        }

        public Builder configurationType(final ConfigurationTypeRef value) {
            this.configurationType = value;
            return this;
        }

        public Builder parameterIndex(final int value) {
            this.parameterIndex = value;
            return this;
        }

        public Builder metadata(final Map<String, String> value) {
            this.metadata = value;
            return this;
        }

        public PropertyDefinition build() {
            return new PropertyDefinition(path, name == null ? path.substring(path.lastIndexOf('.') + 1) : name,
                    displayName, type, defaultValue, placeholder, documentation, tooltip, proposalLabels, constraints,
                    ui, layout, visibility, actions, configurationType, parameterIndex, metadata);
        }
    }
}
