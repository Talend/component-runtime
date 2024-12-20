/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.record;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;

import org.talend.sdk.component.api.record.Schema;

public abstract class Schemas implements Schema, Schema.Builder {

    public static final Schemas STRING = new Schemas() {

        @Override
        public Type getType() {
            return Type.STRING;
        }
    };

    public static final Schemas BYTES = new Schemas() {

        @Override
        public Type getType() {
            return Type.BYTES;
        }
    };

    public static final Schemas INT = new Schemas() {

        @Override
        public Type getType() {
            return Type.INT;
        }
    };

    public static final Schemas LONG = new Schemas() {

        @Override
        public Type getType() {
            return Type.LONG;
        }
    };

    public static final Schemas FLOAT = new Schemas() {

        @Override
        public Type getType() {
            return Type.FLOAT;
        }
    };

    public static final Schemas DOUBLE = new Schemas() {

        @Override
        public Type getType() {
            return Type.DOUBLE;
        }
    };

    public static final Schemas BOOLEAN = new Schemas() {

        @Override
        public Type getType() {
            return Type.BOOLEAN;
        }
    };

    public static final Schemas DATETIME = new Schemas() {

        @Override
        public Type getType() {
            return Type.DATETIME;
        }
    };

    public static final Schemas DECIMAL = new Schemas() {

        @Override
        public Type getType() {
            return Type.DECIMAL;
        }
    };

    public static final Schemas EMPTY_RECORD = new Schemas() {

        @Override
        public Type getType() {
            return Type.RECORD;
        }
    };

    public static Builder valueOf(final String name) {
        switch (name) {
            case "STRING":
                return STRING;
            case "BYTES":
                return BYTES;
            case "INT":
                return INT;
            case "LONG":
                return LONG;
            case "FLOAT":
                return FLOAT;
            case "DOUBLE":
                return DOUBLE;
            case "BOOLEAN":
                return BOOLEAN;
            case "DATETIME":
                return DATETIME;
            case "DECIMAL":
                return DECIMAL;
            case "EMPTY_RECORD":
                return EMPTY_RECORD;
            default:
                throw new IllegalArgumentException(name);
        }
    }

    @Override
    public Schema build() {
        return this;
    }

    @Override
    public Builder withElementSchema(final Schema schema) {
        throw new UnsupportedOperationException("Not allowed for a primitive");
    }

    @Override
    public Builder withType(final Type type) {
        throw new UnsupportedOperationException("Not allowed for a primitive");
    }

    @Override
    public Builder withEntry(final Entry entry) {
        throw new UnsupportedOperationException("Not allowed for a primitive");
    }

    @Override
    public Schema getElementSchema() {
        return null;
    }

    @Override
    public List<Entry> getEntries() {
        return emptyList();
    }

    @Override
    public List<Entry> getMetadata() {
        return emptyList();
    }

    @Override
    @JsonbTransient
    public Stream<Entry> getAllEntries() {
        return Stream.empty();
    }

    @Override
    @JsonbTransient
    public List<Entry> getEntriesOrdered() {
        throw new UnsupportedOperationException("#getEntriesOrdered()");
    }

    @Override
    @JsonbTransient
    public Builder moveBefore(final String before, final String name) {
        throw new UnsupportedOperationException("#moveBefore()");
    }

    @Override
    public Builder moveAfter(final String after, final String name) {
        throw new UnsupportedOperationException("#moveAfter()");
    }

    @Override
    public Builder swap(final String name, final String with) {
        throw new UnsupportedOperationException("#swap()");
    }

    @Override
    public Builder withProps(final Map<String, String> props) {
        throw new UnsupportedOperationException("#withProps()");
    }

    @Override
    public Builder withProp(final String key, final String value) {
        throw new UnsupportedOperationException("#withProp()");
    }

    @Override
    public Map<String, String> getProps() {
        return emptyMap();
    }

    @Override
    public Builder toBuilder() {
        return null;
    }

    @Override
    public String getProp(final String property) {
        throw new UnsupportedOperationException("#getProp()");
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public Builder withEntryAfter(final String before, final Entry entry) {
        throw new UnsupportedOperationException("#withEntryAfter()");
    }

    @Override
    public Builder withEntryBefore(final String after, final Entry entry) {
        throw new UnsupportedOperationException("#withEntryBefore()");
    }

    @Override
    public Builder remove(final String name) {
        throw new UnsupportedOperationException("#remove()");
    }

    @Override
    public Builder remove(final Entry entry) {
        throw new UnsupportedOperationException("#remove()");
    }

    @Override
    public List<Entry> getEntriesOrdered(final Comparator<Entry> comparator) {
        throw new UnsupportedOperationException("#getEntriesOrdered()");
    }

    @Override
    public EntriesOrder naturalOrder() {
        throw new UnsupportedOperationException("#naturalOrder()");
    }
}
