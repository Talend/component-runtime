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
package org.talend.sdk.component.test.rule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.test.Constants;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TempJars implements JUnit5InjectionSupport, AfterAllCallback {

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
        return tmp;
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        Stream.of(new File("target/tempsjars/").listFiles()).forEach(File::delete);
    }

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return Override.class;
    }
}
