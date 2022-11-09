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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.stream.Collectors.toList;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.RecordPointer;
import org.talend.sdk.component.api.record.RecordPointerFactory;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordPointerFactoryImpl implements RecordPointerFactory, Serializable {

    private final String plugin;

    @Override
    public RecordPointer apply(final String pointer) {
        return new RecordPointerImpl(pointer);
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, RecordPointerFactory.class.getName());
    }

    private static class RecordPointerImpl implements RecordPointer, Serializable {

        private final String pointer;

        private final List<String> tokens;

        private RecordPointerImpl(final String pointer) {
            if (pointer == null) {
                throw new NullPointerException("pointer must not be null");
            }
            if (!pointer.equals("") && !pointer.startsWith("/")) {
                throw new IllegalArgumentException("A non-empty pointer string must begin with a '/'");
            }

            this.pointer = pointer;
            this.tokens = Stream.of(pointer.split("/", -1)).map(s -> {
                if (s == null || s.isEmpty()) {
                    return s;
                }
                return s.replace("~1", "/").replace("~0", "~");
            }).collect(toList());
        }

        @Override
        public <T> T getValue(final Record target, final Class<T> type) {
            if (target == null) {
                throw new NullPointerException("target must not be null");
            }
            if (pointer.equals("") || pointer.equals("/")) {
                return type.cast(target);
            }

            Object current = target;
            final int lastIdx = tokens.size() - 1;
            for (int i = 1; i < tokens.size(); i++) {
                current = getValue(current, tokens.get(i), i, lastIdx);
            }
            return type.cast(current);
        }

        public <T> T getEntry(final Record target, final Class<T> type) {
            if (target == null) {
                throw new NullPointerException("target must not be null");
            }
            if (pointer.equals("") || pointer.equals("/")) {
                return type.cast(target);
            }

            Object current = target;
            final int lastIdx = tokens.size() - 1;
            for (int i = 1; i < tokens.size(); i++) {
                current = getValue(current, tokens.get(i), i, lastIdx);
            }
            return type.cast(current);
        }

        private Object getValue(final Object value, final String referenceToken, final int currentPosition,
                final int referencePosition) {
            if (Record.class.isInstance(value)) {
                final Record record = Record.class.cast(value);
                final Object nestedVal = getRecordEntry(referenceToken, record);
                if (nestedVal != null) {
                    return nestedVal;
                }
                throw new IllegalArgumentException(
                        "'" + record + "' contains no value for name '" + referenceToken + "'");
            }
            if (Collection.class.isInstance(value)) {
                if (referenceToken.startsWith("+") || referenceToken.startsWith("-")) {
                    throw new IllegalArgumentException(
                            "An array index must not start with '" + referenceToken.charAt(0) + "'");
                }
                if (referenceToken.startsWith("0") && referenceToken.length() > 1) {
                    throw new IllegalArgumentException("An array index must not start with a leading '0'");
                }

                final Collection<?> array = Collection.class.cast(value);
                try {
                    final int arrayIndex = Integer.parseInt(referenceToken);
                    if (arrayIndex >= array.size()) {
                        throw new IllegalArgumentException(
                                "'" + array + "' contains no element for index " + arrayIndex);
                    }
                    return List.class.isInstance(array) ? List.class.cast(array).get(arrayIndex)
                            : new ArrayList<>(array).get(arrayIndex);
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("'" + referenceToken + "' is no valid array index", e);
                }
            }
            if (currentPosition != referencePosition) {
                return value;
            }
            throw new IllegalArgumentException("'" + value + "' contains no element for '" + referenceToken + "'");
        }

        private Object getRecordEntry(final Schema.Entry entry, final Record record) {
            switch (entry.getType()) {
            case STRING:
                return record.getString(entry.getName());
            case INT:
                return record.getInt(entry.getName());
            case LONG:
                return record.getLong(entry.getName());
            case FLOAT:
                return record.getFloat(entry.getName());
            case DOUBLE:
                return record.getDouble(entry.getName());
            case BOOLEAN:
                return record.getBoolean(entry.getName());
            case BYTES:
                return record.getBytes(entry.getName());
            case DATETIME:
                return record.getDateTime(entry.getName());
            case DECIMAL:
                return record.getDecimal(entry.getName());
            case RECORD:
                return record.getRecord(entry.getName());
            case ARRAY:
                return record.getArray(Object.class, entry.getName());
            default:
                throw new IllegalArgumentException("Unsupported entry type for: " + entry);
            }
        }

        private Object getRecordEntry(final String referenceToken, final Record record) {
            return getEntry(referenceToken, record)
                    .map(entry -> getRecordEntry(entry, record))
                    .orElseGet(() -> record.get(Object.class, referenceToken));
        }

        private Optional<Schema.Entry> getEntry(final String referenceToken, final Record record) {
            return Optional.ofNullable(record.getSchema().getEntry(referenceToken));
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return pointer.equals(RecordPointerImpl.class.cast(obj).pointer);
        }

        @Override
        public int hashCode() {
            return pointer.hashCode();
        }
    }
}
