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
package org.talend.sdk.component.runtime.manager.service;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collector;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.Data;

@Data
public class RecordServiceImpl implements RecordService {

    private final String plugin;

    private final RecordBuilderFactory recordBuilderFactory;

    @Override
    public Collector<Schema.Entry, Record.Builder, Record> toRecord(final Schema schema, final Record fallbackRecord,
            final BiFunction<Schema.Entry, Record.Builder, Boolean> customHandler,
            final BiConsumer<Record.Builder, Boolean> beforeFinish) {
        final AtomicBoolean customHandlerCalled = new AtomicBoolean();
        return Collector.of(() -> recordBuilderFactory.newRecordBuilder(schema), (builder, entry) -> {
            if (!customHandler.apply(entry, builder)) {
                forwardEntry(fallbackRecord, builder, entry.getName(), entry);
            } else {
                customHandlerCalled.set(true);
            }
        }, (b1, b2) -> {
            throw new IllegalStateException("merge unsupported");
        }, builder -> {
            beforeFinish.accept(builder, customHandlerCalled.get());
            return builder.build();
        });
    }

    @Override
    public Record create(final Schema schema, final Record fallbackRecord,
            final BiFunction<Schema.Entry, Record.Builder, Boolean> customHandler,
            final BiConsumer<Record.Builder, Boolean> beforeFinish) {
        return fallbackRecord
                .getSchema()
                .getEntries()
                .stream()
                .collect(toRecord(schema, fallbackRecord, customHandler, beforeFinish));
    }

    @Override
    public boolean forwardEntry(final Record source, final Record.Builder builder, final String sourceColumn,
            final Schema.Entry entry) {
        switch (entry.getType()) {
        case INT:
            final OptionalInt optionalInt = source.getOptionalInt(sourceColumn);
            optionalInt.ifPresent(v -> builder.withInt(entry, v));
            return optionalInt.isPresent();
        case LONG:
            final OptionalLong optionalLong = source.getOptionalLong(sourceColumn);
            optionalLong.ifPresent(v -> builder.withLong(entry, v));
            return optionalLong.isPresent();
        case FLOAT:
            final OptionalDouble optionalFloat = source.getOptionalFloat(sourceColumn);
            optionalFloat.ifPresent(v -> builder.withFloat(entry, (float) v));
            return optionalFloat.isPresent();
        case DOUBLE:
            final OptionalDouble optionalDouble = source.getOptionalDouble(sourceColumn);
            optionalDouble.ifPresent(v -> builder.withDouble(entry, v));
            return optionalDouble.isPresent();
        case BOOLEAN:
            final Optional<Boolean> optionalBoolean = source.getOptionalBoolean(sourceColumn);
            optionalBoolean.ifPresent(v -> builder.withBoolean(entry, v));
            return optionalBoolean.isPresent();
        case STRING:
            final Optional<String> optionalString = source.getOptionalString(sourceColumn);
            optionalString.ifPresent(v -> builder.withString(entry, v));
            return optionalString.isPresent();
        case DATETIME:
            final Optional<ZonedDateTime> optionalDateTime = source.getOptionalDateTime(sourceColumn);
            optionalDateTime.ifPresent(v -> builder.withDateTime(entry, v));
            return optionalDateTime.isPresent();
        case BYTES:
            final Optional<byte[]> optionalBytes = source.getOptionalBytes(sourceColumn);
            optionalBytes.ifPresent(v -> builder.withBytes(entry, v));
            return optionalBytes.isPresent();
        case RECORD:
            final Optional<Record> optionalRecord = source.getOptionalRecord(sourceColumn);
            optionalRecord.ifPresent(v -> builder.withRecord(entry, v));
            return optionalRecord.isPresent();
        case ARRAY:
            final Optional<Collection<Object>> optionalArray = source.getOptionalArray(Object.class, sourceColumn);
            optionalArray.ifPresent(v -> builder.withArray(entry, v));
            return optionalArray.isPresent();
        default:
            throw new IllegalStateException("Unsupported entry type: " + entry);
        }
    }

    Object writeReplace() {
        return new SerializableService(plugin, RecordService.class.getName());
    }
}
