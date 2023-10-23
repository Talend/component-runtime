/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.validator;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.tools.TestRecord;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.talend.sdk.component.api.record.Schema.Type.BYTES;

public class RecordValidatorTest {

    @Test
    void validateErrors() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(MyRecord.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(MyRecord.class));
        assertEquals(2, errors.count());
    }

    /**
     * The result got 3 errors:
     * Method org.talend.sdk.component.tools.TestRecord.withNewSchema(org.talend.sdk.component.api.record.Schema, org.talend.sdk.component.api.record.Record) calls unsafe Builder creator. This either means:
     *   * That the TCK method is safe and should belong to WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER
     * Method org.talend.sdk.component.tools.TestRecord.withNewSchema(org.talend.sdk.component.api.record.Schema) calls unsafe Builder creator. This either means:
     *   * That the TCK method is safe and should belong to WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER
     * Method org.talend.sdk.component.tools.TestRecord.withTestSchema(org.talend.sdk.component.api.record.Schema) calls unsafe Builder creator. This either means:
     *   * That the TCK method is safe and should belong to WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER
     */
    @Test
    void validateTestRecord() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(TestRecord.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(TestRecord.class));
//        errors.forEach( s -> {
//            System.err.println(s);
//        });
        assertEquals(3, errors.count());
    }

    /**
     * The result got one error:
     * Method org.talend.sdk.component.runtime.beam.spi.record.AvroRecord.withNewSchema(org.talend.sdk.component.api.record.Schema) calls unsafe Builder creator. This either means:
     *   * That the TCK method is safe and should belong to WHITE_LIST_TCK_RECORD_BUILDER_PROVIDER
     */
    @Test
    void validateAvroRecord() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(AvroRecord.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(AvroRecord.class));
//        errors.forEach( s -> {
//            System.err.println(s);
//        });
        assertEquals(0, errors.count());
    }

    @Test
    void validateRecordOKs() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(RecordImpl.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(RecordImpl.class));
        assertEquals(0, noerrors.count());
    }

    @Test
    void validateRecordBuilderOKs() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(Record.Builder.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(Record.Builder.class));
        assertEquals(0, noerrors.count());
    }

    class MyRecord implements Record {

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public <T> T get(Class<T> expectedType, String name) {
            return null;
        }

        @Override
        public Builder withNewSchema(final Schema newSchema) {
            return null;
        }

        public Record.Builder newRecordBuilder(final Schema schema) {
            return null;
        }

    }
}
