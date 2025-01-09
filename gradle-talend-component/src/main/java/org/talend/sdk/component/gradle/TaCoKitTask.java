/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.stream.Stream;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

public class TaCoKitTask extends BaseTask {

    protected boolean needsWeb() {
        return false;
    }

    protected Map<String, File> artifacts() {
        final Map<String, File> artifacts = getProject()
                .getConfigurations()
                .getByName("runtime")
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .stream()
                .collect(toMap(this::toGav, ResolvedArtifact::getFile));

        artifacts
                .putIfAbsent(mainGav(),
                        AbstractArchiveTask.class.cast(getProject().getTasks().getAt("jar")).getArchivePath());
        return artifacts;
    }

    protected String mainGav() {
        return String.format("%s:%s:%s", getProject().getGroup(), getProject().getName(), getProject().getVersion());
    }

    protected String toGav(final ResolvedArtifact a) {
        return String
                .format("%s:%s:%s%s:%s:%s", a.getModuleVersion().getId().getGroup(), a.getName(),
                        ofNullable(a.getType()).orElse("jar"),
                        a.getClassifier() == null || a.getClassifier().isEmpty() ? "" : (":" + a.getClassifier()),
                        a.getModuleVersion().getId().getVersion(), "compile");
    }

    protected void executeInContext(final Runnable task) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        final URLClassLoader loader =
                !needsWeb() ? createLoader(old, Stream.of("talendComponentKit", "runtime"), findClasses())
                        : createLoader(old, Stream.of("talendComponentKitWeb", "talendComponentKit"), Stream.empty());
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

    private URLClassLoader createLoader(final ClassLoader parent, final Stream<String> configurations,
            final Stream<File> otherFiles) {
        return new URLClassLoader(Stream
                .concat(configurations.flatMap(n -> getProject().getConfigurations().getByName(n).getFiles().stream()),
                        otherFiles)
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
