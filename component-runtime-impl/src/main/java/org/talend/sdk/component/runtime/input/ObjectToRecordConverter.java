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
package org.talend.sdk.component.runtime.input;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordConverters;

public class ObjectToRecordConverter implements ObjectConverter {

    private static final long serialVersionUID = -3526845195783660333L;

    /** list of "primitive" + String, for auto boxing */
    private static final Set<Class> UNCONVERTED_CLASSES = new HashSet<>(Arrays
            .asList(Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class,
                    Double.class, Void.class, String.class));

    private final RecordConverters converters = new RecordConverters();

    private final RecordConverters.MappingMetaRegistry registry = new RecordConverters.MappingMetaRegistry();

    private transient Jsonb jsonb;

    private transient RecordBuilderFactory recordBuilderFactory;

    @Override
    public Object convert(final ServiceFinder finder, final Object record) {
        if (record == null) {
            return null;
        }
        final Class<?> recordClass = record.getClass();
        if (recordClass.isPrimitive() || UNCONVERTED_CLASSES.contains(recordClass)) {
            // mainly for tests, can be dropped while build is green
            return record;
        }
        return converters.toRecord(registry, record, () -> this.jsonb(finder), () -> this.recordBuilderFactory(finder));
    }

    private Jsonb jsonb(final ServiceFinder finder) {
        if (jsonb == null) {
            synchronized (this) {
                if (jsonb == null) {
                    this.jsonb = finder.findService(Jsonb.class);
                }
            }
        }
        return jsonb;
    }

    private RecordBuilderFactory recordBuilderFactory(final ServiceFinder finder) {
        if (recordBuilderFactory == null) {
            synchronized (this) {
                if (recordBuilderFactory == null) {
                    this.recordBuilderFactory = finder.findService(RecordBuilderFactory.class);
                }
            }
        }
        return recordBuilderFactory;
    }
}
