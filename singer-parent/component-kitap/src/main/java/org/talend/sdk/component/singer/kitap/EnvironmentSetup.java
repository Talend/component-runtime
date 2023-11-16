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
package org.talend.sdk.component.singer.kitap;

import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class EnvironmentSetup {

    static void init() {
        final String baseName = EnvironmentSetup.class.getName();
        if (Boolean.getBoolean(baseName + ".skip")) {
            return;
        }
        if (!Boolean.getBoolean(baseName + ".jul.skip")) {
            jul();
        }
        if (!Boolean.getBoolean(baseName + ".log4j.skip")) {
            log4j();
        }
        if (!Boolean.getBoolean(baseName + ".log4j2.skip")) {
            log4j2();
        }
        if (!Boolean.getBoolean(baseName + ".logback.skip")) {
            logback();
        }
    }

    private static void jul() {
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.SEVERE);
        final Handler[] handlers = root.getHandlers();
        Stream.of(handlers).forEach(root::removeHandler);
        final ConsoleHandler handler = new ConsoleHandler() {

            {
                setFormatter(new UnifiedFormatter());
            }

            @Override
            protected synchronized void setOutputStream(final OutputStream out) throws SecurityException {
                super.setOutputStream(new StdErrStream());
            }
        };
        handler.setLevel(Level.SEVERE);
        root.addHandler(handler);
    }

    private static void logback() {
        System.setProperty("logback.configurationFile", "component-kitap-logback.xml");
    }

    private static void log4j2() {
        System.setProperty("log4j2.configurationFile", "component-kitap-log4j2.xml");
    }

    private static void log4j() {
        System.setProperty("log4j.configuration", "component-kitap-log4j.properties");
    }

    private static class StdErrStream extends OutputStream {

        @Override
        public void write(final int b) {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            System.err.write(b, off, len);
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void close() { // never close gloal stream, it is a JVM responsability
            flush();
        }
    }

    private static class UnifiedFormatter extends Formatter {

        private final ZoneId zone = ZoneId.systemDefault();

        private final ZoneId utc = ZoneId.of("UTC");

        @Override
        public String format(final LogRecord record) {
            final ZonedDateTime zdt =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), zone).withZoneSameInstant(utc);
            final String date = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zdt);
            final String thread =
                    record.getThreadID() == Thread.currentThread().getId() ? Thread.currentThread().getName()
                            : ("thread-" + record.getThreadID());
            final String base = '[' + date + "][" + thread + "][" + record.getLevel().getName() + "]["
                    + record.getLoggerName() + "] " + record.getMessage() + System.lineSeparator();
            final String throwable;
            if (record.getThrown() != null) {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            } else {
                throwable = null;
            }
            return base + (throwable == null ? "" : throwable);
        }
    }
}
