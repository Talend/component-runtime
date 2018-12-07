/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.xbean;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.xbean.finder.archive.Archive;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NestedJarArchive implements Archive, AutoCloseable {

    private final JarInputStream jarStream;

    // this classloader already handles all the logic we need
    private final ConfigurableClassLoader loader;

    @Override
    public InputStream getBytecode(final String className) throws IOException, ClassNotFoundException {
        return loader.getResourceAsStream(className);
    }

    @Override
    public Class<?> loadClass(final String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {

            private ZipEntry entry;

            @Override
            public boolean hasNext() {
                if (entry == null) {
                    try {
                        while ((entry = jarStream.getNextEntry()) != null && !entry.getName().endsWith(".class")) {
                            // next
                        }
                    } catch (final IOException e) {
                        throw new IllegalStateException("Invalid nested jar", e);
                    }
                }
                return entry != null;
            }

            @Override
            public Entry next() {
                final ZipEntry returned = entry;
                entry = null;
                return new Entry() {

                    @Override
                    public String getName() {
                        return returned.getName();
                    }

                    @Override
                    public InputStream getBytecode() {
                        return new KeepingOpenInputStream(jarStream);
                    }
                };
            }
        };
    }

    @Override
    public void close() throws Exception {
        jarStream.close();
    }
}
