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
package org.talend.sdk.component.api.test;

import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@lombok.Builder(setterPrefix = "with",
        builderMethodName = "internalBuilder",
        builderClassName = "MockEntryInternalBuilder")
@lombok.Getter
public class MockEntry implements Schema.Entry {

    private final String name;

    private final String rawName;

    private final String originalFieldName;

    private final Type type;

    private final boolean nullable;

    private final boolean errorCapable;

    private final boolean metadata;

    private final Object defaultVal;

    @Override
    public <T> T getDefaultValue() {
        return (T) defaultVal;
    }

    private final Schema elementSchema;

    private final String comment;

    private final Map<String, String> props;

    @Override
    public String getProp(final String property) {
        return this.getProps().get(property);
    }

    @Override
    public Builder toBuilder() {
        final MockEntryInternalBuilder builder = internalBuilder()
                .withName(name)
                .withRawName(rawName)
                .withOriginalFieldName(originalFieldName)
                .withType(type)
                .withNullable(nullable)
                .withErrorCapable(errorCapable)
                .withMetadata(metadata)
                .withDefaultVal(defaultVal)
                .withElementSchema(elementSchema)
                .withComment(comment)
                .withProps(props == null ? null : new HashMap<>(props));
        return new MockEntryBuilder(builder);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @RequiredArgsConstructor
    public static class MockEntryBuilder implements Builder {

        private final Map<String, String> props = new HashMap<>();

        private final MockEntryInternalBuilder builder;

        @Override
        public Builder withName(String name) {
            builder.withName(name);
            return this;
        }

        @Override
        public Builder withRawName(String rawName) {
            this.builder.withRawName(rawName);
            return this;
        }

        @Override
        public Builder withType(Type type) {
            this.builder.withType(type);
            return this;
        }

        @Override
        public Builder withNullable(boolean nullable) {
            this.builder.withNullable(nullable);
            return this;
        }

        @Override
        public Builder withErrorCapable(boolean errorCapable) {
            this.builder.withErrorCapable(errorCapable);
            return this;
        }

        @Override
        public Builder withMetadata(boolean metadata) {
            this.builder.withMetadata(metadata);
            return this;
        }

        @Override
        public <T> Builder withDefaultValue(T value) {
            builder.withDefaultVal(value);
            return this;
        }

        @Override
        public Builder withElementSchema(Schema schema) {
            builder.withElementSchema(schema);
            return this;
        }

        @Override
        public Builder withComment(String comment) {
            this.builder.withComment(comment);
            return this;
        }

        @Override
        public Builder withProps(Map<String, String> inProps) {
            this.props.clear();
            this.props.putAll(inProps);
            return this;
        }

        @Override
        public Builder withProp(String key, String value) {
            this.props.put(key, value);
            return this;
        }

        @Override
        public Entry build() {
            builder.withProps(this.props);
            return builder.build();
        }
    }
}
