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
package org.talend.sdk.component.junit5.environment;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.talend.sdk.component.junit.environment.Environment;
import org.talend.sdk.component.junit.environment.EnvironmentProvider;

@Environment(MetaEnvironmentsExtensionTest.E1.class)
@Environment(MetaEnvironmentsExtensionTest.E2.class)
@Retention(RUNTIME)
@interface MyEnvs {
}

@MyEnvs
class MetaEnvironmentsExtensionTest {

    private static final List<String> STEPS = new ArrayList<>();

    @BeforeAll
    static void init() {
        STEPS.clear();
    }

    @AfterAll
    static void asserts() {
        assertEquals(asList("start>E1", "beforeEach", "test1", "afterEach", "stop>E1", "start>E2", "beforeEach",
                "test1", "afterEach", "stop>E2", "start>E1", "beforeEach", "test2", "afterEach", "stop>E1", "start>E2",
                "beforeEach", "test2", "afterEach", "stop>E2"), STEPS);
    }

    @BeforeEach
    void beforeEach() {
        STEPS.add("beforeEach");
    }

    @AfterEach
    void afterEach() {
        STEPS.add("afterEach");
    }

    @EnvironmentalTest
    void test1() {
        STEPS.add("test1");
    }

    @EnvironmentalTest
    void test2() {
        STEPS.add("test2");
    }

    public static class E1 implements EnvironmentProvider {

        @Override
        public AutoCloseable start(final Class<?> clazz, final Annotation[] annotations) {
            STEPS.add("start>" + getClass().getSimpleName());
            return () -> STEPS.add("stop>" + E1.class.getSimpleName());
        }
    }

    public static class E2 implements EnvironmentProvider {

        @Override
        public AutoCloseable start(final Class<?> clazz, final Annotation[] annotations) {
            STEPS.add("start>" + getClass().getSimpleName());
            return () -> STEPS.add("stop>" + E2.class.getSimpleName());
        }
    }
}
