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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;

import org.gradle.api.tasks.TaskAction;

public class DocumentationTask extends TaCoKitTask {

    @TaskAction
    public void asciidoc() {
        executeInContext(() -> {
            try {
                doAsciidoc();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void doAsciidoc() throws Exception {
        final TaCoKitExtension extension =
                TaCoKitExtension.class.cast(getProject().getExtensions().findByName("talendComponentKit"));
        if (extension.isSkipDocumentation()) {
            getLogger().info("Documentation is skipped");
            return;
        }

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final Class<?> impl = tccl.loadClass("org.talend.sdk.component.tools.AsciidocDocumentationGenerator");
        final Constructor<?> constructor = impl
                .getConstructor(File[].class, File.class, String.class, int.class, Map.class, Map.class, File.class,
                        String.class, Object.class, File.class, String.class, Locale.class);
        final File[] classes = findClasses().toArray(File[]::new);
        extension.getDocumentationLocales().forEach(locale -> {
            try {
                final String language = locale.toString();
                final File output = ofNullable(extension.getDocumentationOutput()).orElseGet(() -> {
                    final String suffix = language.isEmpty() ? "" : ("_" + language);
                    return new File(getProject().getBuildDir(),
                            "resources/main/TALEND-INF/documentation" + suffix + ".adoc");
                });
                Runnable.class
                        .cast(constructor
                                .newInstance(classes, output,
                                        extension.getDocumentationTitle() == null ? getProject().getName()
                                                : extension.getDocumentationTitle(),
                                        extension.getDocumentationLevel(), extension.getDocumentationFormats(),
                                        extension.getDocumentationAttributes(), extension.getDocumentationTemplateDir(),
                                        extension.getDocumentationTemplateEngine(), getLogger(),
                                        new File(getProject().getBuildDir(), "talend-component/workdir"),
                                        getProject().getVersion().toString(), locale))
                        .run();
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        });
    }
}
