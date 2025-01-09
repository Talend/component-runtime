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
import java.nio.file.Path;

import org.gradle.api.tasks.TaskAction;

public class SVG2PngTask extends TaCoKitTask {

    @TaskAction
    public void convertSVGIconsToPng() {
        executeInContext(() -> {
            final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Class<?> impl = tccl.loadClass("org.talend.sdk.component.tools.SVG2Png");
                final TaCoKitExtension kitExtension = getKitExtension();
                final Runnable runnable = Runnable.class
                        .cast(impl
                                .getConstructor(Path.class, boolean.class, Object.class)
                                .newInstance(getIconsFolder(kitExtension), kitExtension.isUseIconWorkarounds(),
                                        getLogger()));
                runnable.run();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private Path getIconsFolder(final TaCoKitExtension extension) {
        return ofNullable(extension.getIcons())
                .map(File::toPath)
                .orElseGet(() -> getProject().getBuildDir().toPath().resolve("resources/main/icons"));
    }
}
