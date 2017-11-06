/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.asyncvalidation.AsyncValidation;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;

/**
 * Validate ComponentManager constraints to ensure a component doesn't have any "simple" error before being packaged.
 */
@Mojo(name = "validate", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ValidateComponentMojo extends ClasspathMojoBase {

    /**
     * By default the plugin ensures the components are serializable. Skip this validation if this flag is true.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.serializable")
    private boolean validateSerializable;

    /**
     * By default the plugin ensures resource bundle exists and is populated. Skip this validation if this flag is true.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.internationalization")
    private boolean validateInternationalization;

    /**
     * By default the plugin ensures the methods follow the expected signatures. Skipped if this flag is true.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.model")
    private boolean validateModel;

    /**
     * Should {@link org.talend.sdk.component.api.component.Version} and {@link org.talend.sdk.component.api.component.Icon}
     * be enforced.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.metadata")
    private boolean validateMetadata;

    /**
     * Should component model be validated deeply or not (useful for beam components).
     */
    @Parameter(defaultValue = "true", property = "talend.validation.component")
    private boolean validateComponent;

    /**
     * Should datastore healthcheck be enforced.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.datastore")
    private boolean validateDataStore;

    /**
     * Should action signatures be validated.
     */
    @Parameter(defaultValue = "true", property = "talend.validation.action")
    private boolean validateActions;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        final AnnotationFinder finder = newFinder();
        final List<Class<?>> components = componentMarkers().flatMap(a -> finder.findAnnotatedClasses(a).stream())
                .collect(toList());
        components.forEach(c -> getLog().debug("Found component: " + c));

        if (validateSerializable) {
            final Collection<Class<?>> copy = new ArrayList<>(components);
            copy.removeIf(this::isSerializable);
            if (!copy.isEmpty()) {
                copy.forEach(c -> getLog().error(c + " is not Serializable"));
                throw new MojoExecutionException("All components must be serializable for BEAM execution support");

            }
        }

        if (validateInternationalization) {
            final List<String> errors = components.stream().map(this::validateComponentResourceBundle).filter(Objects::nonNull)
                    .collect(toList());
            if (!errors.isEmpty()) {
                errors.forEach(m -> getLog().error(m));
                throw new MojoExecutionException("Some component internationalization is not complete");
            }

            for (final Class<?> i : finder.findAnnotatedClasses(Internationalized.class)) {
                final ResourceBundle resourceBundle = findResourceBundle(i);
                if (resourceBundle != null) {
                    final Collection<String> keys = Stream.of(i.getMethods()).filter(m -> m.getDeclaringClass() != Object.class)
                            .map(m -> i.getName() + "." + m.getName()).collect(toSet());
                    final Collection<String> missingKeys = keys.stream().filter(k -> !resourceBundle.containsKey(k))
                            .collect(toList());
                    if (!missingKeys.isEmpty()) {
                        missingKeys.forEach(k -> getLog().error("Missing key " + k + " in " + i + " resource bundle"));
                        throw new MojoExecutionException(i + " is missing some internalization messages");
                    }

                    final List<String> outdatedKeys = resourceBundle.keySet().stream()
                            .filter(k -> k.startsWith(i.getName() + ".") && !keys.contains(k)).collect(toList());
                    if (!outdatedKeys.isEmpty()) {
                        outdatedKeys.forEach(k -> getLog().error("Key " + k + " from " + i + " is no more used"));
                        throw new MojoExecutionException(i + " has some deleted keys in its resource bundle");
                    }
                } else {
                    getLog().error("No resource bundle for " + i);
                    throw new MojoExecutionException("Missing resource bundle for " + i);
                }
            }
        }

        if (validateModel) {
            final List<Class<?>> ambiguousComponents = components.stream()
                    .filter(c -> componentMarkers().filter(c::isAnnotationPresent).count() > 1).collect(toList());
            if (!ambiguousComponents.isEmpty()) {
                ambiguousComponents
                        .forEach(i -> getLog().error(i + " has conflicting component annotations, ensure it has a single one"));
                throw new MojoExecutionException("Ambiguous components are present: " + ambiguousComponents);
            }

            final ModelVisitor modelVisitor = new ModelVisitor();
            final ModelListener noop = new ModelListener() {
            };
            final List<String> errors = components.stream().map(c -> {
                try {
                    modelVisitor.visit(c, noop, validateComponent);
                    return null;
                } catch (final RuntimeException re) {
                    return re.getMessage();
                }
            }).filter(Objects::nonNull).collect(toList());
            if (!errors.isEmpty()) {
                errors.forEach(e -> getLog().error(e));
                throw new MojoExecutionException("Some model error were detected");
            }
        }

        if (validateMetadata) {
            components.forEach(component -> {
                if (!component.isAnnotationPresent(Version.class) || !component.isAnnotationPresent(Icon.class)) {
                    throw new IllegalArgumentException("Component " + component + " should use @Icon and @Version");
                }
            });
        }

        if (validateDataStore) {
            final Set<String> datastores = finder.findAnnotatedClasses(DataStore.class).stream()
                    .map(d -> d.getAnnotation(DataStore.class).value()).collect(toSet());
            final Set<String> healthchecks = finder.findAnnotatedMethods(HealthCheck.class).stream()
                    .map(m -> m.getAnnotation(HealthCheck.class).value()).collect(toSet());
            if (!healthchecks.containsAll(datastores)) {
                final Set<String> missing = new HashSet<>(datastores);
                datastores.removeAll(healthchecks);
                throw new MojoExecutionException("No @HealthCheck for " + missing + " datastores");
            }
        }

        if (validateActions) {
            final Set<String> errors = new HashSet<>();

            // returned types
            errors.addAll(Stream.of(AsyncValidation.class, DynamicValues.class, HealthCheck.class, DiscoverSchema.class)
                    .flatMap(action -> {
                        final Class<?> returnedType = action.getAnnotation(ActionType.class).expectedReturnedType();
                        return finder.findAnnotatedMethods(action).stream()
                                .filter(m -> !returnedType.isAssignableFrom(m.getReturnType()))
                                .map(m -> m + " doesn't return a " + returnedType + ", please fix it").collect(toSet()).stream();
                    }).collect(toSet()));

            // parameters for @DynamicValues
            errors.addAll(finder.findAnnotatedMethods(DynamicValues.class).stream().filter(m -> countParameters(m) != 0)
                    .map(m -> m + " should have no parameter").collect(toSet()));

            // parameters for @HealthCheck
            errors.addAll(finder.findAnnotatedMethods(HealthCheck.class).stream()
                    .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataStore.class))
                    .map(m -> m + " should have its first parameter being a datastore (marked with @DataStore)")
                    .collect(toSet()));

            // parameters for @DiscoverSchema
            errors.addAll(finder.findAnnotatedMethods(DiscoverSchema.class).stream()
                    .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataSet.class))
                    .map(m -> m + " should have its first parameter being a dataset (marked with @DataSet)").collect(toSet()));

            if (!errors.isEmpty()) {
                throw new MojoExecutionException(errors.stream().collect(joining("\n- ", "\n- ", "")));
            }
        }
    }

    private int countParameters(final Method m) {
        return (int) Stream.of(m.getParameterTypes()).filter(
                p -> !p.getName().startsWith("org.talend.sdk.component.api.service") && !p.isAnnotationPresent(Service.class))
                .count();
    }

    private String validateComponentResourceBundle(final Class<?> component) {
        final String baseName = ofNullable(component.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        final ResourceBundle bundle = findResourceBundle(component);
        if (bundle == null) {
            return "No resource bundle for " + component.getName() + ", you should create a " + baseName.replace('.', '/')
                    + ".properties at least.";
        }

        final String prefix = componentMarkers().map(component::getAnnotation).filter(Objects::nonNull).findFirst()
                .map(this::asComponent).map(c -> findFamily(c, component) + "." + c.name())
                .orElseThrow(() -> new IllegalStateException(component.getName()));
        final Collection<String> missingKeys = Stream.of("_displayName").map(n -> prefix + "." + n)
                .filter(k -> !bundle.containsKey(k)).collect(toList());
        if (!missingKeys.isEmpty()) {
            return baseName + " is missing the keys:" + missingKeys.stream().collect(joining("\n"));
        }
        return null;
    }

    private String findFamily(final Component c, final Class<?> component) {
        return of(c.family()).filter(name -> !name.isEmpty()).orElseGet(() -> {
            final AccessibleClassLoader loader = AccessibleClassLoader.class.cast(Thread.currentThread().getContextClassLoader());
            Package current = component.getPackage();
            do {
                final Components config = current.getAnnotation(Components.class);
                if (config != null) {
                    return config.family();
                }
                final int endPreviousPackage = current.getName().lastIndexOf('.');
                if (endPreviousPackage < 0) {
                    current = null;
                } else {
                    final String parentPck = current.getName().substring(0, endPreviousPackage);
                    try { // try to ensure the package is initialized
                        loader.loadClass(parentPck + ".package-info");
                    } catch (final ClassNotFoundException e) {
                        // no-op
                    }
                    current = loader.findPackage(parentPck);
                }
            } while (current != null);

            throw new IllegalArgumentException("No family for " + component);
        });
    }

    private ResourceBundle findResourceBundle(final Class<?> component) {
        final String baseName = ofNullable(component.getPackage()).map(p -> p.getName() + ".").orElse("") + "Messages";
        try {
            return ResourceBundle.getBundle(baseName, Locale.ENGLISH, Thread.currentThread().getContextClassLoader());
        } catch (final MissingResourceException mre) {
            return null;
        }
    }

    private boolean isSerializable(final Class<?> aClass) {
        return Serializable.class.isAssignableFrom(aClass);
    }

    private Component asComponent(final Annotation a) {
        return Component.class
                .cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { Component.class },
                        (proxy, method, args) -> a.annotationType().getMethod(method.getName()).invoke(a)));
    }

    public interface Component {

        String family();

        String name();
    }
}
