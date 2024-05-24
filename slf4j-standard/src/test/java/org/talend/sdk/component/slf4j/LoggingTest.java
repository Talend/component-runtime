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
package org.talend.sdk.component.slf4j;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingTest {

    private PrintStream out;

    private PrintStream err;

    private ByteArrayOutputStream stdout;

    private ByteArrayOutputStream stderr;

    @BeforeEach
    void redirect() {
        out = System.out;
        err = System.err;
        stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        stderr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(stderr));
    }

    @AfterEach
    void reset() {
        System.setOut(out);
        System.setErr(err);
    }

    @Test
    void info() {
        LoggerFactory.getLogger(LoggingTest.class.getName()).info("test");
        assertEquals("[INFO] test" + lineSeparator(), new String(stdout.toByteArray()));
        assertEquals(0, stderr.size());
    }

    @Test
    void error() {
        LoggerFactory.getLogger(LoggingTest.class.getName()).error("test");
        assertEquals("[ERROR] test" + lineSeparator(), new String(stderr.toByteArray()));
        assertEquals(0, stdout.size());
    }
}
