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
package org.talend.sdk.component.tools.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.RecordImpl;

public class RecordValidatorTest {

    @Test
    void validateErrors() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(MyRecord.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(MyRecord.class));
        assertEquals(2, errors.count());
    }

    @Test
    void validateRecordOKs() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(RecordImpl.class));
        final Stream<String> noerrors =
                validator.validate(finder, Collections.singletonList(RecordImpl.class));
        assertEquals(0, noerrors.count());
    }

    @Test
    void validateRecordBuilderOKs() {
        final RecordValidator validator = new RecordValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(Record.Builder.class));
        final Stream<String> noerrors =
                validator.validate(finder, Collections.singletonList(Builder.class));
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
