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

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.tools.ComponentHelper;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternationalizationValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    private final File sourceRoot;

    private final boolean validatePlaceholder;

    private final boolean autofix;

    public InternationalizationValidator(final ValidatorHelper helper, final File sourceRoot,
            final boolean validatePlaceholder, final boolean autofix) {
        this.helper = helper;
        this.sourceRoot = sourceRoot;
        this.validatePlaceholder = validatePlaceholder;
        this.autofix = autofix;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final Stream<String> bundlesError = components
                .stream() //
                .map(this::validateComponentResourceBundle) //
                .filter(Objects::nonNull) //
                .sorted();

        List<Fix> toFix = new ArrayList<>();
        // enum
        final List<Field> optionsFields = finder.findAnnotatedFields(Option.class);
        final Stream<String> missingDisplayNameEnum = optionsFields
                .stream()
                .map(Field::getType) //
                .filter(Class::isEnum) //
                .distinct() //
                .flatMap(enumType -> of(enumType.getFields()) //
                        .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) //
                        .filter(f -> hasNoBundleEntry(enumType, f, "_displayName")) //
                        .peek(f -> {
                            if (this.autofix) {
                                toFix.add(new Fix(f, "._displayName", sourceRoot, true));
                            }
                        })
                        .map(f -> "Missing key " + enumType.getSimpleName() + "." + f.getName()
                                + "._displayName in " + enumType + " resource bundle")) //
                .sorted();

        // others - just logged for now, we can add it to errors if we encounter it too often
        final List<String> missingOptionTranslations = Stream.concat(
                optionsFields
                        .stream()
                        .distinct()
                        .filter(this::fieldIsWithoutKey)
                        .peek(f -> {
                            if (this.autofix) {
                                toFix.add(new Fix(f, "._displayName", sourceRoot, true));
                            }
                        })
                        .map(f -> " " + f.getDeclaringClass().getSimpleName() + "." + f.getName() + "._displayName = <"
                                + f.getName() + ">")
                        .sorted()
                        .distinct(),
                missingDisplayNameEnum)
                .collect(Collectors.toList());

        List<String> missingPlaceholderTranslations = Collections.emptyList();
        if (this.validatePlaceholder) {
            missingPlaceholderTranslations = optionsFields
                    .stream()
                    .distinct()
                    .filter(e -> this.fieldIsWithoutKey(e,
                            Arrays.asList(String.class, Character.class, Integer.class, Double.class, Long.class,
                                    Float.class, Date.class, ZonedDateTime.class),
                            "._placeholder"))
                    .peek(f -> {
                        if (this.autofix) {
                            toFix.add(new Fix(f, "._placeholder", this.sourceRoot, false));
                        }
                    })
                    .map(f -> " " + f.getDeclaringClass().getSimpleName() + "." + f.getName() + "._placeholder = ")
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (this.autofix && !toFix.isEmpty()) {
            this.fixLocales(toFix);
        }

        final Stream<String> missingDisplayName;
        if (missingOptionTranslations != null && !missingOptionTranslations.isEmpty() && !this.autofix) {
            final String missingMsg = missingOptionTranslations
                    .stream()
                    .collect(Collectors.joining("\n", "Missing _displayName resource bundle entries:\n", ""));
            missingDisplayName = Stream.of(missingMsg);
        } else {
            missingDisplayName = Stream.empty();
        }

        final Stream<String> missingPlaceholder;
        if (missingPlaceholderTranslations != null && !missingPlaceholderTranslations.isEmpty() & !this.autofix) {
            final String missingMsg = missingPlaceholderTranslations
                    .stream()
                    .collect(Collectors.joining("\n", "Missing _placeholder resource bundle entries:\n", ""));
            missingPlaceholder = Stream.of(missingMsg);
        } else {
            missingPlaceholder = Stream.empty();
        }

        final List<String> internationalizedErrors = new ArrayList<>();
        for (final Class<?> i : finder.findAnnotatedClasses(Internationalized.class)) {
            final ResourceBundle resourceBundle = helper.findResourceBundle(i);
            if (resourceBundle != null) {
                final Collection<Collection<String>> keys = of(i.getMethods())
                        .filter(m -> m.getDeclaringClass() != Object.class)
                        .map(m -> asList(i.getName() + "." + m.getName(), i.getSimpleName() + "." + m.getName()))
                        .collect(Collectors.toSet());
                keys
                        .stream()
                        .filter(ks -> ks.stream().noneMatch(resourceBundle::containsKey))
                        .map(k -> "Missing key " + k.iterator().next() + " in " + i + " resource bundle")
                        .sorted()
                        .forEach(internationalizedErrors::add);

                resourceBundle
                        .keySet()
                        .stream()
                        .filter(k -> (k.startsWith(i.getName() + ".") || k.startsWith(i.getSimpleName() + "."))
                                && keys.stream().noneMatch(ks -> ks.contains(k)))
                        .map(k -> "Key " + k + " from " + i + " is no more used")
                        .sorted()
                        .forEach(internationalizedErrors::add);
            } else {
                internationalizedErrors.add("No resource bundle for " + i);
            }
        }
        Stream<String> actionsErrors = this.missingActionComment(finder);

        Stream<String> result = Stream
                .of(bundlesError, missingDisplayName, missingPlaceholder,
                        internationalizedErrors.stream(), actionsErrors)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);

        if (this.autofix) {
            List<String> forLogs = result.collect(toList());
            String resultAutoFix = forLogs.stream()
                    .collect(Collectors.joining("\n", "Automatically fixed missing labels:\n",
                            "\n\nPlease, check changes and disable '-Dtalend.validation.internationalization.autofix=false' / "
                                    + "'<validateInternationalizationAutoFix>false</>'property.\n\n"));
            log.info(resultAutoFix);

            result = forLogs.stream();
        }

        return result;
    }

    private Stream<String> missingActionComment(final AnnotationFinder finder) {
        return this
                .getActionsStream() // Annotation of ActionType
                .flatMap(action -> finder.findAnnotatedMethods(action).stream()) //
                .map(action -> {
                    final Annotation actionAnnotation = Stream
                            .of(action.getAnnotations())
                            .filter(a -> a.annotationType().isAnnotationPresent(ActionType.class))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No action annotation on " + action));
                    final String key;
                    try {
                        final Class<? extends Annotation> annotationType = actionAnnotation.annotationType();
                        key = "${family}.actions." + annotationType.getAnnotation(ActionType.class).value() + "."
                                + annotationType.getMethod("value").invoke(actionAnnotation).toString()
                                + "._displayName";
                    } catch (final IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        return null;
                    }
                    return helper.validateFamilyI18nKey(action.getDeclaringClass(), key);
                })
                .filter(Objects::nonNull);
    }

    private Stream<Class<? extends Annotation>> getActionsStream() {
        return of(AsyncValidation.class, DynamicValues.class, HealthCheck.class, DiscoverSchema.class,
                Suggestions.class, Update.class);
    }

    private String validateComponentResourceBundle(final Class<?> component) {
        final String baseName = ofNullable(component.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        final ResourceBundle bundle = helper.findResourceBundle(component);
        if (bundle == null) {
            return "No resource bundle for " + component.getName() + ", you should create a "
                    + baseName.replace('.', '/') + ".properties at least.";
        }

        final String prefix = this.findPrefix(component);
        final Collection<String> missingKeys =
                of("_displayName").map(n -> prefix + "." + n).filter(k -> !bundle.containsKey(k)).collect(toList());
        if (!missingKeys.isEmpty()) {
            return baseName + " is missing the key(s): " + String.join("\n", missingKeys);
        }
        return null;
    }

    private String findPrefix(final Class<?> component) {
        return ComponentHelper
                .components(component)
                .map(c -> ComponentHelper.findFamily(c, component) + "." + c.name())
                .orElseThrow(() -> new IllegalStateException(component.getName()));
    }

    private boolean hasNoBundleEntry(final Class<?> enumType, final Field f, final String keyName) {
        final ResourceBundle bundle = findBundleFor(enumType, f);
        final String key = enumType.getSimpleName() + "." + f.getName() + "." + keyName;
        return bundle == null || !bundle.containsKey(key);
    }

    private ResourceBundle findBundleFor(final Class<?> enumType, final Field f) {
        return ofNullable(this.helper.findResourceBundle(enumType))
                .orElseGet(() -> this.helper.findResourceBundle(f.getDeclaringClass()));
    }

    private boolean fieldIsWithoutKey(final Field field) {
        return this.fieldIsWithoutKey(field, Collections.emptyList(), "._displayName");
    }

    private boolean fieldIsWithoutKey(final Field field, final List<Class> types, final String suffix) {
        Class<?> tmpFieldType = field.getType();
        if (tmpFieldType.isPrimitive()) {
            tmpFieldType = MethodType.methodType(tmpFieldType).wrap().returnType();
        }
        final Class fieldType = tmpFieldType;
        if (!types.isEmpty() && !types.contains(tmpFieldType)) {
            return false;
        }
        final ResourceBundle bundle = ofNullable(helper.findResourceBundle(field.getDeclaringClass()))
                .orElseGet(() -> helper.findResourceBundle(fieldType));
        final String key = field.getDeclaringClass().getSimpleName() + "." + field.getName() + suffix;
        return bundle == null || !bundle.containsKey(key);
    }

    private void fixLocales(final List<Fix> toFix) {
        Map<Path, List<Fix>> fixByPath = toFix.stream().collect(Collectors.groupingBy(Fix::getDestinationFile));
        for (Path p : fixByPath.keySet()) {
            try {
                Files.createDirectories(p.getParent());
                if (!Files.exists(p)) {
                    Files.createFile(p);
                }
            } catch (IOException e) {
                throw new RuntimeException(String.format("Can't create resource file '%s' : %s", p, e.getMessage()), e);
            }

            List<Fix> fixes = fixByPath.get(p);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(p.toFile(), true))) {
                for (Fix f : fixes) {
                    writer.newLine();
                    writer.write(f.key);
                }
            } catch (IOException e) {
                throw new RuntimeException(String.format("Can't fix internationalization file: '%s'", p), e);
            }

        }
    }

    @Data
    private static class Fix {

        private final String key;

        private final Path destinationFile;

        public Fix(final Field field, final String suffix, final File sourceRoot, final boolean defaultValue) {
            this.key = computeKey(field, suffix, defaultValue);
            this.destinationFile = computeDestinationFile(field, sourceRoot);
        }

        private String computeKey(final Field field, final String suffix, final boolean defaultValue) {
            String s = field.getDeclaringClass().getSimpleName() + "." + field.getName() + suffix + " = ";
            if (defaultValue) {
                s += "<" + field.getName() + ">";
            }

            return s;
        }

        private Path computeDestinationFile(final Field field, final File sourceRoot) {
            String packageName = field.getDeclaringClass().getPackage().getName();
            Path path = Paths.get(sourceRoot.getAbsolutePath())
                    .resolve("src")
                    .resolve("main")
                    .resolve("resources")
                    .resolve(Paths.get(packageName.replaceAll("\\.", "/")))
                    .resolve("Messages.properties");
            return path;
        }
    }

}
