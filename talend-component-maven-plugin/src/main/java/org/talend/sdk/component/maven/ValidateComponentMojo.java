/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.tools.ComponentValidator;

/**
 * Validate ComponentManager constraints to ensure a component doesn't have any
 * "simple" error before being packaged.
 */
@Mojo(name = "validate", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class ValidateComponentMojo extends ClasspathMojoBase {

    /**
     * Ensures each family has an icon.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.family")
    private boolean validateFamily;

    /**
     * By default the plugin ensures the components are serializable. Skip this
     * validation if this flag is true.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.serializable")
    private boolean validateSerializable;

    /**
     * By default the plugin ensures resource bundle exists and is populated. Skip
     * this validation if this flag is true.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.internationalization")
    private boolean validateInternationalization;

    /**
     * By default the plugin ensures the methods follow the expected signatures.
     * Skipped if this flag is true.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.model")
    private boolean validateModel;

    /**
     * Should {@link org.talend.sdk.component.api.component.Version} and
     * {@link org.talend.sdk.component.api.component.Icon} be enforced.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.metadata")
    private boolean validateMetadata;

    /**
     * Should component model be validated deeply or not (useful for beam
     * components).
     */
    @Parameter(defaultValue = "true", property = "talend.validation.component")
    private boolean validateComponent;

    /**
     * Should datastore duplication and healthcheck be enforced.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.datastore")
    private boolean validateDataStore;

    /**
     * Should dataset duplication check be enforced
     */
    @Parameter(defaultValue = "true", property = "talend.validation.dataset")
    private boolean validateDataSet;

    /**
     * Should action signatures be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.action")
    private boolean validateActions;

    /**
     * Should the presence of documentation for the components be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.documentation")
    private boolean validateDocumentation;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        final ComponentValidator.Configuration configuration = new ComponentValidator.Configuration();
        configuration.setValidateFamily(validateFamily);
        configuration.setValidateSerializable(validateSerializable);
        configuration.setValidateInternationalization(validateInternationalization);
        configuration.setValidateModel(validateModel);
        configuration.setValidateMetadata(validateMetadata);
        configuration.setValidateComponent(validateComponent);
        configuration.setValidateDataStore(validateDataStore);
        configuration.setValidateDataSet(validateDataSet);
        configuration.setValidateActions(validateActions);
        configuration.setValidateDocumentation(validateDocumentation);
        new ComponentValidator(configuration, new File[] { classes }, getLog()).run();
    }
}
