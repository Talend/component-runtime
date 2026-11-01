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

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Widget hints of a property. Text values are empty when unspecified; {@code useSeconds} and {@code useUtc} only
 * matter when {@code dateTime} is not {@link DateTimeKind#NONE}.
 */
public record UiHints(
        boolean hidden,
        boolean readOnly,
        boolean credential,
        boolean textArea,
        String codeLanguage,
        DateTimeKind dateTime,
        String dateFormat,
        boolean useSeconds,
        boolean useUtc,
        PathKind path,
        StructureDirection structure,
        String structureConnection,
        String discoverSchema
) {

    public static final UiHints NONE = builder().build();

    public UiHints {
        codeLanguage = Preconditions.nullToEmpty(codeLanguage);
        Preconditions.requireNonNull(dateTime, "dateTime");
        dateFormat = Preconditions.nullToEmpty(dateFormat);
        Preconditions.requireNonNull(path, "path");
        Preconditions.requireNonNull(structure, "structure");
        structureConnection = Preconditions.nullToEmpty(structureConnection);
        discoverSchema = Preconditions.nullToEmpty(discoverSchema);
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum DateTimeKind {
        NONE,
        DATE,
        TIME,
        DATE_TIME,
        ZONED_DATE_TIME
    }

    public enum PathKind {
        NONE,
        FILE,
        DIRECTORY
    }

    /** Role of a schema selector: {@code IN} describes incoming columns, {@code OUT} the produced ones. */
    public enum StructureDirection {
        NONE,
        IN,
        OUT
    }

    public static final class Builder {

        private boolean hidden;

        private boolean readOnly;

        private boolean credential;

        private boolean textArea;

        private String codeLanguage = "";

        private DateTimeKind dateTime = DateTimeKind.NONE;

        private String dateFormat = "";

        private boolean useSeconds = true;

        private boolean useUtc = true;

        private PathKind path = PathKind.NONE;

        private StructureDirection structure = StructureDirection.NONE;

        private String structureConnection = "";

        private String discoverSchema = "";

        private Builder() {
            // use UiHints.builder()
        }

        public Builder hidden(final boolean value) {
            this.hidden = value;
            return this;
        }

        public Builder readOnly(final boolean value) {
            this.readOnly = value;
            return this;
        }

        public Builder credential(final boolean value) {
            this.credential = value;
            return this;
        }

        public Builder textArea(final boolean value) {
            this.textArea = value;
            return this;
        }

        public Builder codeLanguage(final String value) {
            this.codeLanguage = value;
            return this;
        }

        public Builder dateTime(final DateTimeKind value) {
            this.dateTime = value;
            return this;
        }

        public Builder dateFormat(final String value) {
            this.dateFormat = value;
            return this;
        }

        public Builder useSeconds(final boolean value) {
            this.useSeconds = value;
            return this;
        }

        public Builder useUtc(final boolean value) {
            this.useUtc = value;
            return this;
        }

        public Builder path(final PathKind value) {
            this.path = value;
            return this;
        }

        public Builder structure(final StructureDirection direction, final String connection,
                final String discoverSchemaAction) {
            this.structure = direction;
            this.structureConnection = connection;
            this.discoverSchema = discoverSchemaAction;
            return this;
        }

        public UiHints build() {
            return new UiHints(hidden, readOnly, credential, textArea, codeLanguage, dateTime, dateFormat, useSeconds,
                    useUtc, path, structure, structureConnection, discoverSchema);
        }
    }
}
