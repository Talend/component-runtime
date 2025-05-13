/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.record;

public interface SchemaProperty {

    String ORIGIN_TYPE = "field.origin.type";

    String LOGICAL_TYPE = "field.logical.type";

    String SIZE = "field.size";

    String SCALE = "field.scale";

    String PATTERN = "field.pattern";

    String STUDIO_TYPE = "talend.studio.type";

    String IS_KEY = "field.key";

    String IS_FOREIGN_KEY = "field.foreign.key";

    String IS_UNIQUE = "field.unique";

    String ALLOW_SPECIAL_NAME = "field.special.name";

    String ENTRY_IS_ON_ERROR = "entry.on.error";

    String ENTRY_ERROR_MESSAGE = "entry.error.message";

    String ENTRY_ERROR_FALLBACK_VALUE = "entry.error.fallback.value";

    enum LogicalType {

        DATE("date"),
        TIME("time"),
        TIMESTAMP("timestamp"),
        UUID("uuid") {

            @Override
            public Schema.Type storageType() {
                return Schema.Type.STRING;
            }
        };

        private String logicalType;

        LogicalType(final String logicalType) {
            this.logicalType = logicalType;
        }

        public String key() {
            return this.logicalType;
        }

        public Schema.Type storageType() {
            return Schema.Type.DATETIME;
        }
    }

}
