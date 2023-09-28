/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

    enum LogicalType {

        DATE("date", Schema.Type.DATETIME),
        TIME("time", Schema.Type.DATETIME),
        TIMESTAMP("timestamp", Schema.Type.DATETIME),
        UUID("uuid", Schema.Type.STRING);

        private String logicalType;
        private Schema.Type type;

        LogicalType(final String logicalType, final Schema.Type type) {
            this.logicalType = logicalType;
            this.type = type;
        }

        public String key() {
            return this.logicalType;
        }

        public Schema.Type storageType() {
            return this.type;
        }
    }

}
