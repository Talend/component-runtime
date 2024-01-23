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
package org.talend.sdk.component.runtime.standalone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.standalone.RunAtDriver;

public class DriverRunnerImplTest {

    @Test
    public void testLifecycle() {
        final Component delegate = new Component();
        final DriverRunner runner = new DriverRunnerImpl("Root", "Test", "Plugin", delegate);

        assertFalse(delegate.start);
        assertFalse(delegate.stop);
        assertFalse(delegate.run);

        runner.start();
        assertTrue(delegate.start);
        assertFalse(delegate.stop);
        assertFalse(delegate.run);

        runner.runAtDriver();
        assertTrue(delegate.start);
        assertFalse(delegate.stop);
        assertTrue(delegate.run);

        runner.stop();
        assertTrue(delegate.start);
        assertTrue(delegate.stop);
        assertTrue(delegate.run);
    }

    public static class Component implements Serializable {

        private boolean stop;

        private boolean start;

        private boolean run;

        @PostConstruct
        public void init() {
            start = true;
        }

        @RunAtDriver
        public void run() {
            run = true;
        }

        @PreDestroy
        public void destroy() {
            stop = true;
        }
    }

}
