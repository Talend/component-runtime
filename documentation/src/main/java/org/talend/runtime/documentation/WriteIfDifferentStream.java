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
package org.talend.runtime.documentation;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WriteIfDifferentStream extends FilterOutputStream {

    private final File destination;

    public WriteIfDifferentStream(final File destination) {
        super(new ByteArrayOutputStream());
        this.destination = destination;
    }

    @Override
    public void close() throws IOException {
        out.close();
        final byte[] bytes = ByteArrayOutputStream.class.cast(out).toByteArray();
        if (!destination.exists() || isDifferent(bytes)) {
            try (final OutputStream out = new FileOutputStream(destination)) {
                out.write(bytes);
            }
            log.info(destination + " created");
        } else {
            log.info(destination + " didn't change, skip rewriting");
        }
    }

    private boolean isDifferent(final byte[] bytes) throws IOException {
        final String source = Files.lines(destination.toPath()).collect(joining("\n")).trim();
        final String target = new String(bytes, StandardCharsets.UTF_8).trim();
        return !source.equals(target);
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public String toString() {
        return out.toString();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}
