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
package org.talend.sdk.component.runtime.record;

import static java.util.Collections.emptyList;

import java.util.List;

import org.talend.sdk.component.api.record.Schema;

public enum Schemas implements Schema, Schema.Builder {
    STRING {

        @Override
        public Type getType() {
            return Type.STRING;
        }
    },
    BYTES {

        @Override
        public Type getType() {
            return Type.BYTES;
        }
    },
    INT {

        @Override
        public Type getType() {
            return Type.INT;
        }
    },
    LONG {

        @Override
        public Type getType() {
            return Type.LONG;
        }
    },
    FLOAT {

        @Override
        public Type getType() {
            return Type.FLOAT;
        }
    },
    DOUBLE {

        @Override
        public Type getType() {
            return Type.DOUBLE;
        }
    },
    BOOLEAN {

        @Override
        public Type getType() {
            return Type.BOOLEAN;
        }
    },
    DATETIME {

        @Override
        public Type getType() {
            return Type.DATETIME;
        }
    },
    EMPTY_RECORD {

        @Override
        public Type getType() {
            return Type.RECORD;
        }
    };

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
}
