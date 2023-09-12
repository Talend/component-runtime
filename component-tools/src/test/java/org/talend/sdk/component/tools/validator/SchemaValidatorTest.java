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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;

public class SchemaValidatorTest {

    @Test
    void validateErrors() {
        final SchemaValidator validator = new SchemaValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ProcessorImpl.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(ProcessorImpl.class));
        assertEquals(26, noerrors.count());
    }

    @Test
    void validateSchemaOKs() {
        final SchemaValidator validator = new SchemaValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(SchemaImpl.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(SchemaImpl.class));
        assertEquals(0, noerrors.count());
    }

    @Test
    void validateSchemaBuilderOKs() {
        final SchemaValidator validator = new SchemaValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(Schema.Builder.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(Schema.Builder.class));
        assertEquals(0, noerrors.count());
    }

}
