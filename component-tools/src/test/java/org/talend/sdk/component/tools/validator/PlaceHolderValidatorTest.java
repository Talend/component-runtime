/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.ParameterMeta.Source;

import lombok.Data;

class PlaceHolderValidatorTest {

    @Test
    public void test() {
        final PlaceHolderValidator validator = new PlaceHolderValidator(new HelperPlaceHolder());
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(MyClass.class));
        final Stream<String> errors = validator.validate(finder, Arrays.asList(MyClass.class));
        final List<String> errList = errors.collect(Collectors.toList());
        Assertions.assertNotNull(errList);
        Assertions.assertEquals(1, errList.size());
        Assertions
                .assertTrue(errList.get(0).contains("paramKo"),
                        "'" + errList.get(0) + "' doesn't contains path ParamKo");
    }

    @Data
    public static class MyClass {

        private String paramOk;

        private String paramKo;
    }

    @BeforeEach
    public void prepareBundle() {

        ResourceBundle.Control ctrl = new Control() {

            @Override
            public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                    boolean reload) throws IllegalAccessException, InstantiationException, IOException {
                if ("org.talend.sdk.component.tools.validator.Messages".equals(baseName)
                        && "java.properties".equals(format)) {
                    StringReader str = new StringReader("MyClass.paramOk._placeholder=ZZZ\\n");
                    return new PropertyResourceBundle(str);
                }
                return super.newBundle(baseName, locale, format, loader, reload);
            }
        };
        ResourceBundle.getBundle("org.talend.sdk.component.tools.validator.Messages", ctrl);

        ParameterMeta nested = this.buildMeta("paramOk");

        ParameterMeta nested2 = this.buildMeta("paramKo");

        meta = new ParameterMeta(null, MyClass.class, ParameterMeta.Type.OBJECT, "", "MyClass",
                new String[] { MyClass.class.getPackage().getName() }, Arrays.asList(nested, nested2),
                Collections.emptyList(), Collections.emptyMap(), false);
    }

    private ParameterMeta meta;

    private ParameterMeta buildMeta(final String fieldName) {
        return new ParameterMeta(new Source() {

            @Override
            public String name() {
                return fieldName;
            }

            @Override
            public Class<?> declaringClass() {
                return MyClass.class;
            }
        }, String.class, ParameterMeta.Type.STRING, "MyClass." + fieldName, fieldName,
                new String[] { MyClass.class.getPackage().getName() }, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), false);
    }

    private class HelperPlaceHolder extends FakeHelper {

        @Override
        public List<ParameterMeta> buildOrGetParameters(Class<?> c) {
            return Arrays.asList(meta);
        }
    }

}