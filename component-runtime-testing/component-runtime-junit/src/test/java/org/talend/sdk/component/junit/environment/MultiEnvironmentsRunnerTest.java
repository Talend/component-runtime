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
package org.talend.sdk.component.junit.environment;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;

class MultiEnvironmentsRunnerTest {

    private static final List<String> STEPS = new ArrayList<>();

    @org.junit.jupiter.api.Test
    void run() throws Throwable {
        STEPS.clear();
        new AllDefaultPossibilitiesBuilder().runnerForClass(TheTestModel.class).run(new RunNotifier());
        assertEquals(asList("start>E1", "test1", "test2", "stop>E1", "start>E2", "test1", "test2", "stop>E2"), STEPS);
    }

    @RunWith(MultiEnvironmentsRunner.class)
    @Environment(E1.class)
    @Environment(E2.class)
    public static class TheTestModel {

        @Test
        public void test1() {
            STEPS.add("test1");
        }

        @Test
        public void test2() {
            STEPS.add("test2");
        }
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
