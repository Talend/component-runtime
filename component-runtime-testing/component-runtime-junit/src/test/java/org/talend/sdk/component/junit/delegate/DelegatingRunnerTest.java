/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.delegate;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

class DelegatingRunnerTest {

    @Test
    void run() throws Throwable {
        MyRunner.STEPS.clear();
        new AllDefaultPossibilitiesBuilder().runnerForClass(TheTestModel.class).run(new RunNotifier());
        assertEquals(asList("constructor>" + TheTestModel.class.getName(), "description", "run"), MyRunner.STEPS);
    }

    @RunWith(DelegatingRunner.class)
    @DelegateRunWith(MyRunner.class)
    public static class TheTestModel {
    }

    public static class MyRunner extends Runner {

        private static final Collection<String> STEPS = new ArrayList<>();

        public MyRunner(final Class<?> type) {
            STEPS.add("constructor>" + type.getName());
        }

        @Override
        public Description getDescription() {
            STEPS.add("description");
            return Description.EMPTY;
        }

        @Override
        public void run(final RunNotifier notifier) {
            STEPS.add("run");
        }
    }
}
