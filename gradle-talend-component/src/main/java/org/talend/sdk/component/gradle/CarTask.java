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
package org.talend.sdk.component.gradle;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.util.Map;

import org.gradle.api.tasks.TaskAction;

public class CarTask extends TaCoKitTask {

    @TaskAction
    public void createCar() {
        executeInContext(() -> {
            try {
                doCar();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void doCar() throws Exception {
        final TaCoKitExtension extension = getKitExtension();

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final Class<?> configImpl = tccl.loadClass("org.talend.sdk.component.tools.CarBundler$Configuration");
        final Class<?> impl = tccl.loadClass("org.talend.sdk.component.tools.CarBundler");

        final Object config = configImpl.getConstructor().newInstance();
        config.getClass().getMethod("setMainGav", String.class).invoke(config, mainGav());
        config
                .getClass()
                .getMethod("setVersion", String.class)
                .invoke(config, String.valueOf(getProject().getVersion()));
        config.getClass().getMethod("setArtifacts", Map.class).invoke(config, artifacts());
        config.getClass().getMethod("setCustomMetadata", Map.class).invoke(config, extension.getCarMetadata());
        config
                .getClass()
                .getMethod("setOutput", File.class)
                .invoke(config, ofNullable(extension.getCarOutput())
                        .orElseGet(
                                () -> new File(getProject().getBuildDir(), "libs/" + getProject().getName() + ".car")));

        Runnable.class.cast(impl.getConstructor(configImpl, Object.class).newInstance(config, getLogger())).run();
    }
}
