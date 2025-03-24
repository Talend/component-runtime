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
package org.talend.sdk.component.tools.validator;

import static java.util.stream.Stream.of;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.tools.ComponentValidator.Configuration;
import org.talend.sdk.component.tools.spi.ValidationExtension;

public class Validators {

    private final List<Validator> validators;

    private Validators(final List<Validator> validators) {
        this.validators = validators;
    }

    public interface ValidatorHelper {

        boolean isService(Parameter parameter);

        ResourceBundle findResourceBundle(final Class<?> component);

        String validateFamilyI18nKey(final Class<?> clazz, final String... keys);

        List<ParameterMeta> buildOrGetParameters(final Class<?> c);

        String validateIcon(final Icon annotation, final Collection<String> errors);

        ParameterModelService getParameterModelService();

        Stream<File> componentClassFiles();
    }

    public Set<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final Set<String> errors = new LinkedHashSet<>();

        this.validators
                .stream()
                .flatMap((Validator validator) -> validator.validate(finder, components))
                .forEach(errors::add);
        return errors;
    }

    public static Validators build(final Configuration configuration, final ValidatorHelper helper,
            final Iterable<ValidationExtension> extensions, final File sourceRoot) {

        final List<Validator> activeValidators = new ArrayList<>();

        if (configuration.isValidateSerializable()) {
            activeValidators.add(new SerializationValidator());
        }

        if (configuration.isValidateInternationalization()) {
            InternationalizationValidator intern = new InternationalizationValidator(helper, sourceRoot,
                    configuration.isValidatePlaceholder(), configuration.isValidateInternationalizationAutoFix());
            activeValidators.add(intern);
        }

        if (configuration.isValidateHttpClient()) {
            activeValidators.add(new HttpValidator());
        }

        if (configuration.isValidateModel()) {
            final ModelValidator modelValidator = new ModelValidator(configuration.isValidateComponent(), helper);
            activeValidators.add(modelValidator);
        }

        if (configuration.isValidateMetadata()) {
            final MetadataValidator metaValidator = new MetadataValidator(helper);
            activeValidators.add(metaValidator);
        }

        if (configuration.isValidateDataStore()) {
            final DataStoreValidator validator = new DataStoreValidator(helper);
            activeValidators.add(validator);
        }

        if (configuration.isValidateDataSet()) {
            final DatasetValidator validator = new DatasetValidator(helper);
            activeValidators.add(validator);
            final DatasetDiscoveryValidator discoveryValidator = new DatasetDiscoveryValidator(helper);
            activeValidators.add(discoveryValidator);
        }

        if (configuration.isValidateActions()) {
            final ActionValidator validator = new ActionValidator(helper);
            activeValidators.add(validator);
        }

        if (configuration.isValidateDocumentation()) {
            activeValidators.add(new DocumentationValidator());
        }

        if (configuration.isValidateLayout()) {
            final LayoutValidator validator = new LayoutValidator(helper);
            activeValidators.add(validator);
        }
        if (configuration.isValidateOptionNames()) {
            activeValidators.add(new OptionNameValidator());
            activeValidators.add(new OptionParameterValidator());
        }

        if (configuration.isValidateLocalConfiguration()) {
            final LocalConfigurationValidator validator = new LocalConfigurationValidator(helper, configuration);
            activeValidators.add(validator);
        }
        if (configuration.isValidateOutputConnection()) {
            activeValidators.add(new OutputConnectionValidator());
        }
        if (configuration.isValidateNoFinalOption()) {
            activeValidators.add(new NoFinalOptionValidator());
        }
        if (configuration.isValidateWording()) {
            if (configuration.isValidateDocumentation()) {
                activeValidators.add(new DocumentationWordingValidator());
            }
        }

        if (configuration.isValidateExceptions()) {
            final ExceptionValidator validator = new ExceptionValidator(helper, configuration);
            activeValidators.add(validator);
        }

        if (configuration.isValidateFamily()) {
            final FamilyValidator validator = new FamilyValidator(helper);
            activeValidators.add(validator);
        }
        if (extensions != null) {
            extensions.forEach((ValidationExtension ext) -> {
                final ExtensionValidator validator = new ExtensionValidator(ext, helper);
                activeValidators.add(validator);
            });
        }
        if (configuration.isValidateRecord()) {
            activeValidators.add(new RecordValidator());
        }
        if (configuration.isValidateSchema()) {
            activeValidators.add(new SchemaValidator());
        }
        if (configuration.isValidateFixedSchema()) {
            activeValidators.add(new FixedSchemaValidator());
        }
        if (configuration.isValidateCheckpoint()) {
            activeValidators.add(new CheckpointValidator(helper));
        }

        return new Validators(activeValidators);

    };

    public static Stream<Class<? extends Annotation>> getActionsStream() {
        return of(AsyncValidation.class, DynamicValues.class, HealthCheck.class, DiscoverSchema.class,
                Suggestions.class, Update.class, DynamicDependencies.class);
    }

    public static Stream<ParameterMeta> flatten(final Collection<ParameterMeta> options) {
        return options
                .stream()
                .flatMap((ParameterMeta it) -> Stream
                        .concat(Stream.of(it), it.getNestedParameters().isEmpty() ? Stream.empty()
                                : Validators.flatten(it.getNestedParameters())));
    }
}
