/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.component.test.rule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.rules.ExternalResource;
import org.talend.component.test.Constants;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TempJars extends ExternalResource {

    private final Collection<File> toClean = new ArrayList<>();

    public File create(final String dependencies) {
        final File tmp = new File("target/tempsjars/" + UUID.randomUUID().toString() + ".jar");
        if (!tmp.getParentFile().exists() && !tmp.getParentFile().mkdirs()) {
            throw new IllegalArgumentException("Check you can write in " + tmp.getParentFile());
        }

        try (final JarOutputStream jar = new JarOutputStream(new FileOutputStream(tmp))) {
            jar.putNextEntry(new ZipEntry(Constants.DEPENDENCIES_LIST_RESOURCE_PATH));
            jar.write(dependencies.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        toClean.add(tmp);
        return tmp;
    }

    @Override
    protected void after() {
        toClean.forEach(f -> {
            if (!f.delete()) { // on win it can fail
                f.deleteOnExit();
            }
        });
    }
}
