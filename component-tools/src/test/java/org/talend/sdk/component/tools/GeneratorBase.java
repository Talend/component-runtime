/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

abstract class GeneratorBase {

    protected File findWorkDir() {
        return new File(jarLocation(GeneratorBase.class).getParentFile(), getClass().getSimpleName() + "_workdir");
    }

    protected File copyBinaries(final String pck, final File tmp, final String name) {
        final String pckPath = pck.replace('.', '/');
        final File root = new File(jarLocation(getClass()), pckPath);
        final File scannable = new File(tmp, getClass().getName() + "_" + name);
        final File classDir = new File(scannable, pckPath);
        classDir.mkdirs();
        ofNullable(root.listFiles())
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .filter(c -> c.getName().endsWith(".class") || c.getName().endsWith(".properties"))
                .forEach(c -> {
                    try {
                        Files.copy(c.toPath(), new File(classDir, c.getName()).toPath());
                    } catch (final IOException e) {
                        fail("cant create test plugin");
                    }
                });
        return scannable;
    }
}
