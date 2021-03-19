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
package org.talend.sdk.component.runtime.manager.xbean;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarFile;

import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.JarArchive;

public class CloseableJarArchive implements Archive, AutoCloseable {

    private final JarArchive delegate;

    public CloseableJarArchive(final ClassLoader loader, final URL url) {
        this.delegate = new JarArchive(loader, url);
    }

    @Override
    public void close() throws Exception {
        Field[] fields = delegate.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(JarFile.class)) {
                field.setAccessible(true);
                Object obj = field.get(delegate);
                ((JarFile) obj).close();
            }
        }
    }

    @Override
    public InputStream getBytecode(final String s) throws IOException, ClassNotFoundException {
        return delegate.getBytecode(s);
    }

    @Override
    public Class<?> loadClass(final String s) throws ClassNotFoundException {
        return delegate.loadClass(s);
    }

    @Override
    public Iterator<Entry> iterator() {
        return delegate.iterator();
    }

    public URL getUrl() {
        return delegate.getUrl();
    }
}
