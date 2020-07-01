/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.record;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import routines.system.Dynamic;
import routines.system.DynamicMetadata;
import routines.system.DynamicMetadata.sourceTypes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.service.record.RecordVisitor;

import lombok.Data;

@Data
public class DiRecordVisitor implements RecordVisitor<Object> {

    private final Class<?> clazz;

    private final Object instance;

    private final Map<String, Field> fields;

    private final boolean hasDynamic;

    private final Dynamic dynamic;

    private final String dynamicColumn;

    DiRecordVisitor(final Class<?> clzz) {
        clazz = clzz;
        try {
            instance = clazz.getConstructor().newInstance();
            fields = Arrays.stream(instance.getClass().getFields()).collect(toMap(Field::getName, identity()));
            hasDynamic = fields
                    .values()
                    .stream()
                    .anyMatch(field -> "routines.system.Dynamic".equals(field.getType().getName()));
            dynamicColumn = fields
                    .values()
                    .stream()
                    .filter(field -> "routines.system.Dynamic".equals(field.getType().getName()))
                    .map(Field::getName)
                    .findAny()
                    .orElse(null);
            if (hasDynamic) {
                dynamic = new Dynamic();
            } else {
                dynamic = null;
            }
        } catch (final NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object get() {
        if (hasDynamic) {
            try {
                fields.get(dynamicColumn).set(instance, dynamic);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    private void setField(final Entry entry, final Object value) {
        Field field = fields.get(entry.getName());
        if (hasDynamic && field == null) {
            final DynamicMetadata metadata = new DynamicMetadata();
            metadata.setName(entry.getName());
            metadata.setDbName(entry.getOriginalFieldName());
            metadata.setNullable(entry.isNullable());
            metadata.setDescription(entry.getComment());
            metadata.setKey(false);
            metadata.setSourceType(sourceTypes.unknown);
            metadata.setLength(100);
            metadata.setPrecision(0);
            switch (entry.getType()) {
            case RECORD:
                metadata.setType("id_Object");
                break;
            case ARRAY:
                metadata.setType("id_List");
                break;
            case STRING:
                metadata.setType("id_String");
                break;
            case BYTES:
                metadata.setType("id_Byte");
                break;
            case INT:
                metadata.setType("id_Integer");
                break;
            case LONG:
                metadata.setType("id_Long");
                break;
            case FLOAT:
                metadata.setType("id_Float");
                metadata.setLength(10);
                metadata.setPrecision(5);
                break;
            case DOUBLE:
                metadata.setType("id_Double");
                metadata.setLength(20);
                metadata.setPrecision(10);
                break;
            case BOOLEAN:
                metadata.setType("id_Boolean");
                break;
            case DATETIME:
                metadata.setType("id_Date");
                metadata.setLogicalType("timestamp-millis");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entry.getType());
            }
            dynamic.metadatas.add(metadata);
            dynamic.addColumnValue(value);
            return;
        }
        try {
            field.set(instance, value);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onInt(final Entry entry, final OptionalInt optionalInt) {
        optionalInt.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onLong(final Entry entry, final OptionalLong optionalLong) {
        optionalLong.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onFloat(final Entry entry, final OptionalDouble optionalFloat) {
        optionalFloat.ifPresent(value -> setField(entry, (float) value));
    }

    @Override
    public void onDouble(final Entry entry, final OptionalDouble optionalDouble) {
        optionalDouble.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBoolean(final Entry entry, final Optional<Boolean> optionalBoolean) {
        optionalBoolean.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onString(final Entry entry, final Optional<String> string) {
        string.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDatetime(final Entry entry, final Optional<ZonedDateTime> dateTime) {
        dateTime.ifPresent(value -> setField(entry, value.toInstant().toEpochMilli()));
    }

    @Override
    public void onBytes(final Entry entry, final Optional<byte[]> bytes) {
        onString(entry, Optional.of(Base64.getEncoder().encodeToString(bytes.orElse(new byte[] {}))));
    }

    @Override
    public void onIntArray(final Entry entry, final Optional<Collection<Integer>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onLongArray(final Entry entry, final Optional<Collection<Long>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onFloatArray(final Entry entry, final Optional<Collection<Float>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDoubleArray(final Entry entry, final Optional<Collection<Double>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBooleanArray(final Entry entry, final Optional<Collection<Boolean>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onStringArray(final Entry entry, final Optional<Collection<String>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDatetimeArray(final Entry entry, final Optional<Collection<ZonedDateTime>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBytesArray(final Entry entry, final Optional<Collection<byte[]>> array) {
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public RecordVisitor<Object> onRecordArray(final Entry entry, final Optional<Collection<Record>> array) {
        // for now...
        return null;
    }

    @Override
    public RecordVisitor<Object> onRecord(final Entry entry, final Optional<Record> record) {
        // for now...
        return null;
    }

}
