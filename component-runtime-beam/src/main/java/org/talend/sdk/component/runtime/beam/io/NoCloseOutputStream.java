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
package org.talend.sdk.component.runtime.beam.io;

import java.io.IOException;
import java.io.OutputStream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NoCloseOutputStream extends OutputStream /* not the filter version for perf reasons */ {

    private final OutputStream in;

    @Override
    public void close() throws IOException {
        // no-pop
    }

    @Override
    public void write(final int b) throws IOException {
        in.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        in.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        in.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        in.flush();
    }
}
