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
package org.talend.sdk.component.tools;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

import java.io.File;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;
import org.talend.sdk.component.tools.spi.ValidationExtension;
import org.talend.sdk.component.tools.validator.Validators;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

import lombok.Data;

// IMPORTANT: this class is used by reflection in gradle integration, don't break signatures without checking it
public class ComponentValidator extends BaseTask {

    public static final String ICONS = "icons" + File.separator;

    private final Configuration configuration;

    private final Log log;

    private final ParameterModelService parameterModelService =
            new ParameterModelService(new EnrichedPropertyEditorRegistry());

    private final SvgValidator validator;

    private final Map<Class<?>, List<ParameterMeta>> parametersCache = new HashMap<>();

    private final List<ValidationExtension> extensions;

    private final File sourceRoot;

    public ComponentValidator(final Configuration configuration, final File[] classes, final Object log,
            final File sourceRoot) {
        super(classes);
        this.configuration = configuration;
        this.sourceRoot = sourceRoot;
        this.validator = new SvgValidator(this.configuration.isValidateLegacyIcons());

        try {
            this.log = Log.class.isInstance(log) ? Log.class.cast(log) : new ReflectiveLog(log);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        this.extensions = StreamSupport
                .stream(ServiceLoader.load(ValidationExtension.class).spliterator(), false)
                .collect(toList());
    }

    @Override
    public void run() {
        final AnnotationFinder finder = newFinder();
        final List<Class<?>> components = ComponentHelper
                .componentMarkers()
                .flatMap(a -> finder.findAnnotatedClasses(a).stream())
                .collect(toList());
        components.forEach(c -> log.debug("Found component: " + c));

        final Set<String> errors = new LinkedHashSet<>();
        final Validators.ValidatorHelper helper = new ValidatorHelper() {

            @Override
            public boolean isService(final Parameter parameter) {
                return ComponentValidator.this.parameterModelService
                        .isService(new ParameterModelService.Param(parameter));
            }

            @Override
            public ResourceBundle findResourceBundle(final Class<?> component) {
                return ComponentValidator.this.findResourceBundle(component);
            }

            @Override
            public String validateFamilyI18nKey(final Class<?> clazz, final String... keys) {
                return ComponentValidator.this.validateFamilyI18nKey(clazz, keys);
            }

            @Override
            public List<ParameterMeta> buildOrGetParameters(final Class<?> c) {
                return ComponentValidator.this.buildOrGetParameters(c);
            }

            @Override
            public String validateIcon(final Icon annotation, final Collection<String> errors) {
                return ComponentValidator.this.validateIcon(annotation, errors);
            }

            @Override
            public ParameterModelService getParameterModelService() {
                return ComponentValidator.this.parameterModelService;
            }

            @Override
            public Stream<File> componentClassFiles() {
                if (ComponentValidator.this.classes == null) {
                    return Stream.empty();
                }
                return Stream.of(ComponentValidator.this.classes);
            }
        };

        final Validators validators = Validators.build(configuration, helper, extensions, sourceRoot);
        final Set<String> errorsFromValidator = validators.validate(finder, components);
        errors.addAll(errorsFromValidator);

        if (!errors.isEmpty()) {
            final List<String> preparedErrors =
                    errors.stream().map(it -> it.replace("java.lang.", "").replace("java.util.", "")).collect(toList());
            preparedErrors.forEach(log::error);
            throw new IllegalStateException(
                    "Some error were detected:" + preparedErrors.stream().collect(joining("\n- ", "\n- ", "")));
        }

        log.info("Validated components: " + components.stream().map(Class::getSimpleName).collect(joining(", ")));
    }

    private String validateIcon(final Icon annotation, final Collection<String> errors) {
        if (classes.length == 0) {
            return null;
        }

        if (annotation.value() == Icon.IconType.CUSTOM) {
            final String icon = annotation.custom();
            Set<File> svgs;
            // legacy checks
            if (configuration.isValidateLegacyIcons()) {
                svgs = of(classes)
                        .map(it -> new File(it, ICONS + icon + ".svg"))
                        .collect(toSet());
            } else {
                // themed icons check
                List<String> prefixes = new ArrayList<>();
                of(classes).forEach(s -> {
                    prefixes.add(s + File.separator + ICONS + "light" + File.separator + icon);
                    prefixes.add(s + File.separator + ICONS + "dark" + File.separator + icon);
                });
                svgs = prefixes.stream().map(s -> new File(s + ".svg")).collect(toSet());
            }

            if (configuration.isValidateSvg()) {
                errors.addAll(svgs.stream().filter(File::exists).flatMap(this::validateSvg).collect(toSet()));
            }

            List<File> missingSvgs = svgs.stream().filter(f -> !f.exists()).collect(toList());
            if (!missingSvgs.isEmpty()) {
                errors.addAll(missingSvgs.stream()
                        .map(p -> String.format(
                                "No '%s' found.",
                                stripPath(p)))
                        .collect(toList()));
                return "Missing icon(s) in resources:";
            }
        }
        return null;
    }

    private String stripPath(final File icon) {
        try {
            return icon.toString().substring(icon.toString().indexOf(ICONS));
        } catch (StringIndexOutOfBoundsException e) {
            log.error("Validate Icon Path Error :" + icon.toString() + "-- Exception: " + e.getMessage());
        }
        return ("Icon Path Error :" + icon.toString());
    }

    private Stream<String> validateSvg(final File file) {
        return validator.validate(file.toPath());
    }

    private List<ParameterMeta> buildOrGetParameters(final Class<?> c) {
        return parametersCache
                .computeIfAbsent(c,
                        k -> parameterModelService
                                .buildParameterMetas(findConstructor(c),
                                        ofNullable(c.getPackage()).map(Package::getName).orElse(""),
                                        new BaseParameterEnricher.Context(
                                                new LocalConfigurationService(emptyList(), "tools"))));
    }

    private String validateFamilyI18nKey(final Class<?> clazz, final String... keys) {
        final Class<?> pck =
                ComponentHelper.findPackageOrFail(clazz, apiTester(Components.class), Components.class.getName());
        final String family = pck.getAnnotation(Components.class).family();
        final String baseName = ofNullable(pck.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        final ResourceBundle bundle = findResourceBundle(pck);
        if (bundle == null) {
            return "No resource bundle for " + clazz.getName() + " translations, you should create a "
                    + baseName.replace('.', '/') + ".properties at least.";
        }

        final Collection<String> missingKeys = of(keys)
                .map(key -> key.replace("${family}", family))
                .filter(k -> !bundle.containsKey(k))
                .collect(toList());
        if (!missingKeys.isEmpty()) {
            return baseName + " is missing the key(s): " + String.join("\n", missingKeys);
        }
        return null;
    }

    private static <T> Collector<T, ?, Set<T>> toSet() {
        return toCollection(TreeSet::new);
    }

    @Data
    public static class Configuration {

        private boolean validateFamily;

        private boolean validateSerializable;

        private boolean validateInternationalization;

        private boolean validateInternationalizationAutoFix;

        private boolean validateHttpClient;

        private boolean validateModel;

        private boolean validateMetadata;

        private boolean validateComponent;

        private boolean validateDataStore;

        private boolean validateDataSet;

        private boolean validateActions;

        private boolean validateDocumentation;

        private boolean validateWording;

        private boolean validateLayout;

        private boolean validateOptionNames;

        private boolean validateLocalConfiguration;

        private boolean validateOutputConnection;

        private boolean validatePlaceholder;

        private boolean validateSvg;

        private boolean validateLegacyIcons;

        private boolean validateNoFinalOption;

        private String pluginId;

        private boolean validateExceptions;

        private boolean failOnValidateExceptions;

        private boolean validateRecord;

        private boolean validateSchema;

        private boolean validateFixedSchema;

    }
}
