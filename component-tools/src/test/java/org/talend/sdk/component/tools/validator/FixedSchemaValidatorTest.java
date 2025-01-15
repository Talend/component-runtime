/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.api.service.schema.FixedSchema;

public class FixedSchemaValidatorTest {

    @Test
    void validateFixedSchema() {
        final FixedSchemaValidator validator = new FixedSchemaValidator();
        AnnotationFinder finder =
                new AnnotationFinder(new ClassesArchive(MySourceOk.class, MySourceOkExt.class, FixedService.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(MySourceOk.class, MySourceOkExt.class, FixedService.class));
        assertEquals(0, noerrors.count());

        finder = new AnnotationFinder(
                new ClassesArchive(MySourceKoEmpty.class, MySourceKoMissing.class, FixedService.class));
        final Stream<String> errors = validator.validate(finder,
                Arrays.asList(MySourceKoEmpty.class, MySourceKoMissing.class, FixedService.class));
        assertEquals(2, errors.count());
    }

    @Emitter(family = "test", name = "mysource0")
    @FixedSchema("discover")
    static class MySourceOk implements Serializable {

        @Producer
        public Record next() {
            return null;
        }
    }

    @Emitter(family = "test", name = "mysource1")
    @FixedSchema("discoverext")
    static class MySourceOkExt implements Serializable {

        @Producer
        public Record next() {
            return null;
        }
    }

    @Emitter(family = "test", name = "mysource2")
    @FixedSchema
    static class MySourceKoEmpty implements Serializable {

        @Producer
        public Record next() {
            return null;
        }
    }

    @Emitter(family = "test", name = "mysource3")
    @FixedSchema("missing")
    static class MySourceKoMissing implements Serializable {

        @Producer
        public Record next() {
            return null;
        }
    }

    @DataSet("ds")
    public static class DS {

        @Option
        private String dto;
    }

    @Service
    static class FixedService {

        @DiscoverSchema("discover")
        public Schema discover(DS ds) {
            return null;
        }

        @DiscoverSchemaExtended("discoverext")
        public Schema discover(DS ds, String branch) {
            return null;
        }
    }
}
