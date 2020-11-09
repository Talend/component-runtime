package org.talend.sdk.component.tools.validator;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.tools.ComponentValidator.Configuration;

public class Validators {
    private final List<Validator> validators;

    private Validators(List<Validator> validators) {
        this.validators = validators;
    }

    public interface ValidatorHelper {
        boolean isService(Parameter parameter);

        ResourceBundle findResourceBundle(final Class<?> component);

        String findPrefix(final Class<?> component);

        String validateFamilyI18nKey(final Class<?> clazz, final String... keys);

        List<ParameterMeta> buildOrGetParameters(final Class<?> c);

        String validateIcon(final Icon annotation, final Collection<String> errors);

        ParameterModelService getParameterModelService();
    }

    public Set<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final Set<String> errors = new LinkedHashSet<>();

        this.validators.stream()
                .flatMap((Validator validator) -> validator.validate(finder, components))
                .forEach(errors::add);
        return errors;
    }

    public static Validators build(final Configuration configuration,
            final ValidatorHelper helper) {

        final List<Validator> activeValidators = new ArrayList<>();

        if (configuration.isValidateSerializable()) {
            activeValidators.add(new SerializationValidator());
        }

        if (configuration.isValidateInternationalization()) {
            InternationalizationValidator intern = new InternationalizationValidator(helper);
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
        }

        if (configuration.isValidateDocumentation()) {
            activeValidators.add(new DocumentationValidator());
        }


        return new Validators(activeValidators);

    };

}
