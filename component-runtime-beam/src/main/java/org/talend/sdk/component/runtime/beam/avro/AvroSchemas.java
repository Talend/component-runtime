/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.avro;

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

import org.apache.avro.Schema;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class AvroSchemas {

    private static final org.apache.avro.Schema EMPTY_SCHEMA = org.apache.avro.Schema
            .createRecord("org.talend.sdk.component.schema.generated.EmptyRecord", null, null, false);
    static {
        EMPTY_SCHEMA.setFields(emptyList());
    }

    public static Schema unwrapUnion(final Schema schema) {
        switch (schema.getType()) {
        case UNION:
            return schema.getTypes().get(schema.getTypes().size() - 1);
        default:
            return schema;
        }
    }

    public static Schema getEmptySchema() {
        return EMPTY_SCHEMA;
    }

    public static String sanitizeConnectionName(final String name) {
        if (name.isEmpty()) {
            return name;
        }
        final char[] original = name.toCharArray();
        final boolean skipFirstChar = !Character.isLetter(original[0]) && original[0] != '_';
        final int offset = skipFirstChar ? 1 : 0;
        final char[] sanitized = skipFirstChar ? new char[original.length - offset] : new char[original.length];
        if (!skipFirstChar) {
            sanitized[0] = original[0];
        }
        for (int i = 1; i < original.length; i++) {
            if (!Character.isLetterOrDigit(original[i]) && original[i] != '_') {
                sanitized[i - offset] = '_';
            } else {
                sanitized[i - offset] = original[i];
            }
        }
        return new String(sanitized);
    }
}
