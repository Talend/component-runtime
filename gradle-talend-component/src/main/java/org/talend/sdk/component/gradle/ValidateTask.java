/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import java.lang.reflect.InvocationTargetException;

import org.gradle.api.tasks.TaskAction;

public class ValidateTask extends TaCoKitTask {

    @TaskAction
    public void validateTalendComponents() {
        executeInContext(() -> {
            try {
                doValidateTalendComponents();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void doValidateTalendComponents() throws Exception {
        final TaCoKitExtension extension =
                TaCoKitExtension.class.cast(getProject().getExtensions().findByName("talendComponentKit"));
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final Class<?> config = tccl.loadClass("org.talend.sdk.component.tools.ComponentValidator$Configuration");
        final Object configuration = config.getConstructor().newInstance();

        set(configuration, "setValidateFamily", extension.isValidateFamily());
        set(configuration, "setValidateSerializable", extension.isValidateSerializable());
        set(configuration, "setValidateInternationalization", extension.isValidateInternationalization());
        set(configuration, "setValidateModel", extension.isValidateModel());
        set(configuration, "setValidateMetadata", extension.isValidateMetadata());
        set(configuration, "setValidateComponent", extension.isValidateComponent());
        set(configuration, "setValidateDataStore", extension.isValidateDataStore());
        set(configuration, "setValidateDataSet", extension.isValidateDataSet());
        set(configuration, "setValidateActions", extension.isValidateActions());
        set(configuration, "setValidateDocumentation", extension.isValidateDocumentation());
        set(configuration, "setValidateLayout", extension.isValidateLayout());
        set(configuration, "setValidateOptionNames", extension.isValidateOptionNames());
        set(configuration, "setValidateLocalConfiguration", extension.isValidateLocalConfiguration());
        set(configuration, "setValidateOutputConnection", extension.isValidateOutputConnection());
        set(configuration, "setValidatePlaceholder", extension.isValidatePlaceholder());
        set(configuration, "setValidateSvg", extension.isValidateSvg());
        set(configuration, "setValidateNoFinalOption", extension.isValidateNoFinalOption());
        configuration
                .getClass()
                .getMethod("setPluginId", String.class)
                .invoke(configuration, ofNullable(extension.getPluginId()).orElseGet(getProject()::getName));

        final Class<?> validator = tccl.loadClass("org.talend.sdk.component.tools.ComponentValidator");
        final Runnable runnable = Runnable.class
                .cast(validator
                        .getConstructor(config, File[].class, Object.class)
                        .newInstance(configuration, findClasses().toArray(File[]::new), getLogger()));
        runnable.run();
    }

    private void set(final Object configuration, final String mtd, final boolean value)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        configuration.getClass().getMethod(mtd, boolean.class).invoke(configuration, value);
    }
}
