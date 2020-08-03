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

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import routines.system.Dynamic;
import routines.system.DynamicMetadata;
import routines.system.DynamicMetadata.sourceTypes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.api.service.record.RecordVisitor;
import org.talend.sdk.component.runtime.manager.service.DefaultServiceProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DiRecordVisitor implements RecordVisitor<Object> {

    private final Class<?> clazz;

    private Object instance;

    private final Map<String, Field> fields;

    private final boolean hasDynamic;

    private final Dynamic dynamic;

    private final String dynamicColumn;

    private String recordPrefix = "";

    private String arrayOfRecordPrefix = "";

    private Set<String> recordFields;

    private static final RecordService RECORD_SERVICE = RecordService.class
            .cast(new DefaultServiceProvider(null, JsonProvider.provider(), Json.createGeneratorFactory(emptyMap()),
                    Json.createReaderFactory(emptyMap()), Json.createBuilderFactory(emptyMap()),
                    Json.createParserFactory(emptyMap()), Json.createWriterFactory(emptyMap()), new JsonbConfig(),
                    JsonbProvider.provider(), null, null, emptyList(), t -> new RecordBuilderFactoryImpl("di"), null)
                            .lookup(null, Thread.currentThread().getContextClassLoader(), null, null,
                                    RecordService.class, null));

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
            log.debug("[DiRecordVisitor] Class: {} has dynamic: {} ({}).", clazz.getName(), hasDynamic, dynamicColumn);
        } catch (final NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object visit(final Record record) {
        arrayOfRecordPrefix = "";
        recordPrefix = "";
        try {
            instance = clazz.getConstructor().newInstance();
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (hasDynamic) {
            dynamic.metadatas.clear();
            dynamic.clearColumnValues();
        }
        recordFields =
                record.getSchema().getEntries().stream().filter(t -> t.getType().equals(Type.RECORD)).map(rcdEntry -> {
                    final String root = rcdEntry.getName() + ".";
                    final List<String> names = new ArrayList<>();
                    rcdEntry
                            .getElementSchema()
                            .getEntries()
                            .stream()
                            .filter(e -> e.getType().equals(Type.RECORD))
                            .map(sr -> {
                                final String sub = root + sr.getName() + ".";
                                return sr
                                        .getElementSchema()
                                        .getEntries()
                                        .stream()
                                        .map(entry -> sub + entry.getName())
                                        .collect(Collectors.toList());
                            })
                            .forEach(l -> l.stream().forEach(m -> names.add(m)));
                    rcdEntry
                            .getElementSchema()
                            .getEntries()
                            .stream()
                            .filter(e -> !e.getType().equals(Type.RECORD))
                            .map(entry -> root + entry.getName())
                            .forEach(sre -> names.add(sre));
                    return names;
                }).flatMap(liststream -> liststream.stream()).collect(Collectors.toSet());
        recordFields
                .addAll(record
                        .getSchema()
                        .getEntries()
                        .stream()
                        .filter(t -> !t.getType().equals(Type.RECORD))
                        .map(entry -> entry.getName())
                        .collect(Collectors.toSet()));
        return RECORD_SERVICE.visit(this, record);
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
            metadata
                    .setName(recordFields
                            .stream()
                            .filter(f -> f.endsWith("." + entry.getName()))
                            .findFirst()
                            .orElse(entry.getName()));
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
            log.debug("[setField] Dynamic {}\t({})\t ==> {}.", metadata.getName(), metadata.getType(), value);
            return;
        }
        if (field == null) {
            log.warn("[setField] Apparently {} has no schema defined.", clazz.getName());
            return;
        }
        try {
            field.set(instance, coerce(field.getType(), value, entry.getName()));
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> T coerce(final Class<T> expectedType, final Object value, final String name) {
        if (value == null) {
            return null;
        }

        // datetime cases
        if (Long.class.isInstance(value) && expectedType != Long.class) {
            if (expectedType == ZonedDateTime.class) {
                final long epochMilli = Number.class.cast(value).longValue();
                if (epochMilli == -1L) { // not <0 which can be a bug
                    return null;
                }
                return expectedType.cast(ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), UTC));
            }
            if (expectedType == Date.class) {
                return expectedType.cast(new Date(Number.class.cast(value).longValue()));
            }
        }

        if (!expectedType.isInstance(value)) {
            if (Number.class.isInstance(value) && Number.class.isAssignableFrom(expectedType)) {
                return mapNumber(expectedType, Number.class.cast(value));
            }
            if (String.class.isInstance(value)) {
                if (ZonedDateTime.class == expectedType) {
                    return expectedType.cast(ZonedDateTime.parse(String.valueOf(value)));
                }
                if (char.class == expectedType || Character.class == expectedType) {
                    return expectedType.cast(String.valueOf(value).charAt(0));
                }
                if (byte[].class == expectedType) {
                    return expectedType.cast(Base64.getDecoder().decode(String.valueOf(value)));
                }
                if (BigDecimal.class == expectedType) {
                    return expectedType.cast(new BigDecimal(String.valueOf(value)));
                }
            }

            throw new IllegalArgumentException(name + " can't be converted to " + expectedType);
        }

        return expectedType.cast(value);
    }

    public <T> T mapNumber(final Class<T> expected, final Number from) {
        if (expected == Double.class || expected == double.class) {
            return expected.cast(from.doubleValue());
        }
        if (expected == Float.class || expected == float.class) {
            return expected.cast(from.floatValue());
        }
        if (expected == Integer.class || expected == int.class) {
            return expected.cast(from.intValue());
        }
        if (expected == Long.class || expected == long.class) {
            return expected.cast(from.longValue());
        }
        if (expected == Byte.class || expected == byte.class) {
            return expected.cast(from.byteValue());
        }
        if (expected == Short.class || expected == short.class) {
            return expected.cast(from.shortValue());
        }
        throw new IllegalArgumentException("Can't convert " + from + " to " + expected);
    }

    @Override
    public void onInt(final Entry entry, final OptionalInt optionalInt) {
        log.debug("[onInt] visiting {}.", entry.getName());
        optionalInt.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onLong(final Entry entry, final OptionalLong optionalLong) {
        log.debug("[onLong] visiting {}.", entry.getName());
        optionalLong.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onFloat(final Entry entry, final OptionalDouble optionalFloat) {
        log.debug("[onFloat] visiting {}.", entry.getName());
        optionalFloat.ifPresent(value -> setField(entry, (float) value));
    }

    @Override
    public void onDouble(final Entry entry, final OptionalDouble optionalDouble) {
        log.debug("[onDouble] visiting {}.", entry.getName());
        optionalDouble.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBoolean(final Entry entry, final Optional<Boolean> optionalBoolean) {
        log.debug("[onBoolean] visiting {}.", entry.getName());
        optionalBoolean.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onString(final Entry entry, final Optional<String> string) {
        log.debug("[onString] visiting {}.", entry.getName());
        string.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDatetime(final Entry entry, final Optional<ZonedDateTime> dateTime) {
        log.debug("[onDatetime] visiting {}.", entry.getName());
        dateTime.ifPresent(value -> setField(entry, value.toInstant().toEpochMilli()));
    }

    @Override
    public void onBytes(final Entry entry, final Optional<byte[]> bytes) {
        log.debug("[onBytes] visiting {}.", entry.getName());
        onString(entry, Optional.of(Base64.getEncoder().encodeToString(bytes.orElse(new byte[] {}))));
    }

    @Override
    public void onIntArray(final Entry entry, final Optional<Collection<Integer>> array) {
        log.debug("[onIntArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onLongArray(final Entry entry, final Optional<Collection<Long>> array) {
        log.debug("[onLongArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onFloatArray(final Entry entry, final Optional<Collection<Float>> array) {
        log.debug("[onFloatArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDoubleArray(final Entry entry, final Optional<Collection<Double>> array) {
        log.debug("[onDoubleArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBooleanArray(final Entry entry, final Optional<Collection<Boolean>> array) {
        log.debug("[onBooleanArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onStringArray(final Entry entry, final Optional<Collection<String>> array) {
        log.debug("[onStringArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDatetimeArray(final Entry entry, final Optional<Collection<ZonedDateTime>> array) {
        log.debug("[onDatetimeArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBytesArray(final Entry entry, final Optional<Collection<byte[]>> array) {
        log.debug("[onBytesArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public RecordVisitor<Object> onRecordArray(final Entry entry, final Optional<Collection<Record>> array) {
        log.debug("[onRecordArray] visiting {}.", entry.getName());
        arrayOfRecordPrefix = entry.getName() + ".";
        return this;
    }

    @Override
    public RecordVisitor<Object> onRecord(final Entry entry, final Optional<Record> record) {
        log.debug("[onRecord] visiting {}.", entry.getName());
        recordPrefix = entry.getName() + ".";
        return this;
    }

}
