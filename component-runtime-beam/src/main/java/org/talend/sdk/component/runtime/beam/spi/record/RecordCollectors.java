/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.spi.record;

import static lombok.AccessLevel.PRIVATE;

import org.talend.sdk.component.api.record.Record;

import lombok.NoArgsConstructor;

// collector related utilities
@NoArgsConstructor(access = PRIVATE)
public final class RecordCollectors {

    public static void merge(final Record.Builder b1, final Record.Builder b2) {
        final Record toInclude = b2.build();
        toInclude.getSchema().getAllEntries().forEach(e -> {
            switch (e.getType()) {
            case RECORD:
                b1.withRecord(e, toInclude.getRecord(e.getName()));
                break;
            case ARRAY:
                b1.withArray(e, toInclude.getArray(Object.class, e.getName()));
                break;
            case DATETIME:
                b1.withDateTime(e, toInclude.getDateTime(e.getName()));
                break;
            case DECIMAL:
                b1.withDecimal(e, toInclude.getDecimal(e.getName()));
                break;
            case BYTES:
                b1.withBytes(e, toInclude.getBytes(e.getName()));
                break;
            case STRING:
                b1.withString(e, toInclude.getString(e.getName()));
                break;
            case DOUBLE:
                b1.withDouble(e, toInclude.getDouble(e.getName()));
                break;
            case INT:
                b1.withInt(e, toInclude.getInt(e.getName()));
                break;
            case LONG:
                b1.withLong(e, toInclude.getLong(e.getName()));
                break;
            case FLOAT:
                b1.withFloat(e, toInclude.getFloat(e.getName()));
                break;
            case BOOLEAN:
                b1.withBoolean(e, toInclude.getBoolean(e.getName()));
                break;
            default:
                throw new IllegalArgumentException("Unsupported: " + e.getType());
            }
        });
    }
}
