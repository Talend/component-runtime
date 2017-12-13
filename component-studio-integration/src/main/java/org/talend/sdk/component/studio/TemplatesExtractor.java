/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

package org.talend.sdk.component.studio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.FrameworkUtil;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TemplatesExtractor {

    private final String templatesPath;

    private final String destinationFolder;

    public void extract() throws IOException, URISyntaxException {
        final File jarFile = FileLocator.getBundleFile(FrameworkUtil.getBundle(this.getClass()));
        final File destDir = new File(destinationFolder, "tacokit");

        if (jarFile.isFile()) { // Run with JAR file
            try (JarFile jar = new JarFile(jarFile)) {
                final Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
                while (entries.hasMoreElements()) {
                    JarEntry file = (JarEntry) entries.nextElement();
                    if (!file.getName().startsWith(templatesPath)) {
                        continue;
                    }
                    File dst = new File(destDir, file.getName());
                    if (file.isDirectory()) { // if its a directory, create it
                        createDirectory(dst);
                    } else {
                        extractFile(jar, file, dst);
                    }
                }
            }
        }
    }

    private void createDirectory(final File dir) {
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getAbsolutePath() + " should not exist or should be a directory.");
        } else if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void extractFile(final JarFile jar, final JarEntry src, final File dest) throws IOException {
        try (InputStream fis = jar.getInputStream(src); FileOutputStream fos = new FileOutputStream(dest)) {
            while (fis.available() > 0) {
                fos.write(fis.read());
            }
        }
    }

}
