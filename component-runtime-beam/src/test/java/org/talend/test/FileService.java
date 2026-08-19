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
package org.talend.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.sdk.component.api.service.Service;

@Service
public class FileService {

    private final ConcurrentMap<String, WriterRef> writers = new ConcurrentHashMap<>();

    public synchronized Writer writerFor(final String path) {
        final WriterRef ref = writers.computeIfAbsent(path, p -> {
            try {
                final WriterRef writerRef = new WriterRef();
                writerRef.writer = new FileWriter(p) {

                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            synchronized (writerRef) {
                                writerRef.count--;
                                if (writerRef.count == 0) {
                                    writers.remove(path);
                                }
                            }
                        }
                    }
                };
                return writerRef;
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
        synchronized (ref) {
            ref.count++;
        }
        return ref.writer;
    }

    private static class WriterRef {

        private Writer writer;

        private volatile int count;
    }
}
