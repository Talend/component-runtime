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
package org.talend.sdk.component.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.PUBLIC;

import java.io.File;
import java.util.Locale;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.ComponentValidator;

/**
 * Validate ComponentManager constraints to ensure a component doesn't have any
 * "simple" error before being packaged.
 */
@Audience(PUBLIC)
@Mojo(name = "validate", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class ValidateComponentMojo extends ClasspathMojoBase {

    /**
     * Ensures string variables have a placeholder.
     */
    @Parameter(defaultValue = "false" /* backward compatibility */, property = "talend.validation.placeholder")
    private boolean validatePlaceholder;

    /**
     * Ensures SVG icons comply to recommended rules.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.svg")
    private boolean validateSvg;

    /*
     * Ensures icons according theme or legacy mode
     */
    @Parameter(defaultValue = "false", property = "talend.validation.icons.legacy")
    private boolean validateLegacyIcons;

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

    @Parameter(defaultValue = "false", property = "talend.validation.internationalization.autofix")
    private boolean validateInternationalizationAutoFix;

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

    /**
     * Should the Talend rules of wording for the components be validated.
     */
    @Parameter(defaultValue = "false", property = "talend.validation.wording")
    private boolean validateWording;

    /**
     * Should the layout of the component be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.layout")
    private boolean validateLayout;

    /**
     * Should the option names be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.options")
    private boolean validateOptionNames;

    /**
     * Ensure output component has only one single input branch.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.validateOutputConnection")
    private boolean validateOutputConnection;

    /**
     * Should the option names be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.localConfiguration")
    private boolean validateLocalConfiguration;

    /**
     * Should the option names be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.noFinalOption")
    private boolean validateNoFinalOption;

    /**
     * Should the exceptions thrown be validated
     */
    @Parameter(defaultValue = "true", property = "talend.validation.exceptions")
    private boolean validateExceptions;

    /**
     * Should build fail on exception validation error
     */
    @Parameter(defaultValue = "false", property = "talend.validation.failOnValidateExceptions")
    private boolean failOnValidateExceptions;

    /**
     * Should the record be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.record")
    private boolean validateRecord;

    /**
     * Should the schema be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.schema")
    private boolean validateSchema;

    /**
     * Should the option names be validated.
     */
    @Parameter(defaultValue = "${project.artifactId}", property = "talend.validation.pluginId")
    private String pluginId;

    /**
     * Which locale to use to validate internationalization.
     */
    @Parameter(defaultValue = "${project.artifactId}", property = "talend.validation.locale")
    private String locale;

    @Override
    public void doExecute() {
        if (!validatePlaceholder) {
            getLog()
                    .warn("You don't validate placeholders are set, maybe think about setting validatePlaceholder=true");
        }
        if (!validateWording) {
            getLog().warn("You don't validate wording rules, maybe think about setting validateWording=true");
        }
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
        configuration.setValidateLayout(validateLayout);
        configuration.setValidateOptionNames(validateOptionNames);
        configuration.setValidateLocalConfiguration(validateLocalConfiguration);
        configuration.setValidateOutputConnection(validateOutputConnection);
        configuration.setValidatePlaceholder(validatePlaceholder);
        configuration.setValidateSvg(validateSvg);
        configuration.setValidateLegacyIcons(validateLegacyIcons);
        configuration.setValidateNoFinalOption(validateNoFinalOption);
        configuration.setValidateWording(validateWording);
        configuration.setPluginId(pluginId);
        configuration.setValidateExceptions(validateExceptions);
        configuration.setFailOnValidateExceptions(failOnValidateExceptions);
        configuration.setValidateSchema(validateSchema);
        configuration.setValidateRecord(validateRecord);
        configuration.setValidateInternationalizationAutoFix(validateInternationalizationAutoFix);

        final Locale locale = this.locale == null || "root".equals(this.locale) ? Locale.ROOT : new Locale(this.locale);

        new ComponentValidator(configuration, new File[] { classes }, getLog(), project.getBasedir()) {

            @Override
            protected Locale getLocale() {
                return locale;
            }
        }.run();

    }
}
