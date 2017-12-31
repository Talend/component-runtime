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
package org.talend.sdk.component.gradle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;

public class TaCoKitTask extends DefaultTask {

    protected void executeInContext(final Runnable task) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        final URLClassLoader loader = createLoader(old);
        try {
            thread.setContextClassLoader(loader);
            task.run();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        } finally {
            thread.setContextClassLoader(old);
            try {
                loader.close();
            } catch (final IOException e) {
                getLogger().error(e.getMessage(), e);
            }
        }
    }

    private URLClassLoader createLoader(final ClassLoader parent) {
        return new URLClassLoader(Stream
                .concat(Stream.concat(
                        getProject().getConfigurations().getByName("talendComponentKit").getFiles().stream(),
                        getProject().getConfigurations().getByName("runtime").getFiles().stream()), findClasses())
                .distinct()
                .map(f -> {
                    try {
                        return f.toURI().toURL();
                    } catch (final MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(URL[]::new), parent);
    }

    protected Stream<File> findClasses() {
        return Stream
                .of("classes/main", "classes/java/main", "resources/main")
                .map(p -> new File(getProject().getBuildDir(), p))
                .filter(File::exists);
    }
}
