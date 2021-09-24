/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.singer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;

class IOTest {

    @Test
    void stashIo() throws UnsupportedEncodingException {
        final IO testIO = new IO();
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final PrintStream stdoutPs = new PrintStream(stdout);
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        final PrintStream stderrPs = new PrintStream(stderr);
        final IO overridenIO = new IO(new InputStream() {

            @Override
            public int read() {
                return -1;
            }
        }, stdoutPs, stderrPs);
        overridenIO.set();
        try {
            System.out.println("test out stash");
            stdoutPs.flush();

            System.err.println("test err stash");
            stderrPs.flush();

            assertEquals("test out stash" + System.lineSeparator(), stdout.toString("UTF-8"));
            assertEquals("test err stash" + System.lineSeparator(), stderr.toString("UTF-8"));
        } finally {
            testIO.set();
        }
    }
}
