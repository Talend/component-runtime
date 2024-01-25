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
package org.talend.sdk.component.tools.validator;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

public class InternationalizationValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public InternationalizationValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final Stream<String> bundlesError = components
                .stream() //
                .map(this::validateComponentResourceBundle) //
                .filter(Objects::nonNull) //
                .sorted();

        // enum
        final List<Field> optionsFields = finder.findAnnotatedFields(Option.class);
        final Stream<String> missingDisplayName = optionsFields
                .stream()
                .map(Field::getType) //
                .filter(Class::isEnum) //
                .distinct() //
                .flatMap(enumType -> Stream
                        .of(enumType.getFields()) //
                        .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) //
                        .filter(f -> hasNoBundleEntry(enumType, f, "_displayName")) //
                        .map(f -> "Missing key " + enumType.getSimpleName() + "." + f.getName() + "._displayName in "
                                + enumType + " resource bundle")) //
                .sorted();

        // others - just logged for now, we can add it to errors if we encounter it too often
        final List<String> missingOptionTranslations = optionsFields
                .stream()
                .distinct()
                .filter(this::fieldIsWithoutKey)
                .map(f -> " " + f.getDeclaringClass().getSimpleName() + "." + f.getName() + "._displayName = <"
                        + f.getName() + ">")
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        final Stream<String> allMissing;
        if (missingOptionTranslations != null && !missingOptionTranslations.isEmpty()) {
            final String missingMsg = missingOptionTranslations
                    .stream()
                    .collect(Collectors.joining("\n", "Missing resource bundle entries:\n", ""));
            allMissing = Stream.of(missingMsg);
        } else {
            allMissing = Stream.empty();
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

        return Stream
                .of(bundlesError, missingDisplayName, allMissing, internationalizedErrors.stream(), actionsErrors)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
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
        final ResourceBundle bundle = ofNullable(helper.findResourceBundle(field.getDeclaringClass()))
                .orElseGet(() -> helper.findResourceBundle(field.getType()));
        final String key = field.getDeclaringClass().getSimpleName() + "." + field.getName() + "._displayName";
        return bundle == null || !bundle.containsKey(key);
    }
}
