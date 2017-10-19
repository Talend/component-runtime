// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager.xbean;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.xbean.finder.archive.Archive;
import org.talend.components.classloader.ConfigurableClassLoader;

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
                    public InputStream getBytecode() throws IOException {
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
