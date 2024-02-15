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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.discovery.DiscoverDataset;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class ActionValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public ActionValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        // returned types
        final Stream<String> actionType = this.checkActionType(finder);

        // parameters for @DynamicValues
        final Stream<String> actionWithoutParameter = finder
                .findAnnotatedMethods(DynamicValues.class)
                .stream()
                .filter(m -> countParameters(m) != 0)
                .map(m -> m + " should have no parameter")
                .sorted();

        // parameters for @HealthCheck
        final Stream<String> health = finder
                .findAnnotatedMethods(HealthCheck.class)
                .stream()
                .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataStore.class))
                .map(m -> m + " should have its first parameter being a datastore (marked with @DataStore)")
                .sorted();

        // Discover dataset
        final Stream<String> datasetDiscover = finder
                .findAnnotatedMethods(DiscoverDataset.class)
                .stream()
                .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataStore.class))
                .map(m -> m + " should have a datastore as first parameter (marked with @DataStore)")
                .sorted();

        // parameters for @DiscoverSchema
        final Stream<String> discover = finder
                .findAnnotatedMethods(DiscoverSchema.class)
                .stream()
                .filter(m -> countParameters(m) != 1 || !m.getParameterTypes()[0].isAnnotationPresent(DataSet.class))
                .map(m -> m + " should have its first parameter being a dataset (marked with @DataSet)")
                .sorted();

        // parameters for @DiscoverSchemaExtended
        final Stream<String> discoverProcessor = findDiscoverSchemaExtendedErrors(finder);

        // returned type for @Update, for now limit it on objects and not primitives
        final Stream<String> updatesErrors = this.findUpdatesErrors(finder);

        final Stream<String> enumProposable = finder
                .findAnnotatedFields(Proposable.class)
                .stream()
                .filter(f -> f.getType().isEnum())
                .map(f -> f.toString() + " must not define @Proposable since it is an enum")
                .sorted();

        final Set<String> proposables = finder
                .findAnnotatedFields(Proposable.class)
                .stream()
                .map(f -> f.getAnnotation(Proposable.class).value())
                .collect(Collectors.toSet());
        final Set<String> dynamicValues = finder
                .findAnnotatedMethods(DynamicValues.class)
                .stream()
                .map(f -> f.getAnnotation(DynamicValues.class).value())
                .collect(Collectors.toSet());
        proposables.removeAll(dynamicValues);

        final Stream<String> proposableWithoutDynamic = proposables
                .stream()
                .map(p -> "No @DynamicValues(\"" + p + "\"), add a service with this method: " + "@DynamicValues(\"" + p
                        + "\") Values proposals();")
                .sorted();

        return Stream
                .of(actionType, //
                        actionWithoutParameter, //
                        health, //
                        datasetDiscover, //
                        discover, //
                        discoverProcessor, //
                        updatesErrors, //
                        enumProposable, //
                        proposableWithoutDynamic) //
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);

    }

    private Stream<String> checkActionType(final AnnotationFinder finder) {
        return Validators.getActionsStream().flatMap(action -> {
            final Class<?> returnedType = action.getAnnotation(ActionType.class).expectedReturnedType();
            final List<Method> annotatedMethods = finder.findAnnotatedMethods(action);
            return Stream
                    .concat(annotatedMethods
                            .stream()
                            .filter(m -> !returnedType.isAssignableFrom(m.getReturnType()))
                            .map(m -> m + " doesn't return a " + returnedType + ", please fix it"),
                            annotatedMethods
                                    .stream()
                                    .filter(m -> !m.getDeclaringClass().isAnnotationPresent(Service.class)
                                            && !Modifier.isAbstract(m.getDeclaringClass().getModifiers()))
                                    .map(m -> m + " is not declared into a service class"));
        }).sorted();
    }

    /**
     * Checks method signature for @DiscoverSchemaExtended annotation.
     * Valid signatures are:
     * <ul>
     * <li>public Schema guessMethodName(final Schema incomingSchema, final @Option("configuration") procConf, final
     * String branch)</li>
     * <li>public Schema guessMethodName(final Schema incomingSchema, final @Option("configuration") procConf)</li>
     * <li>public Schema guessMethodName(final @Option("configuration") procConf, final String branch)</li>
     * <li>public Schema guessMethodName(final @Option("configuration") procConf)</li>
     * </ul>
     *
     * @param finder
     * @return Errors on @DiscoverSchemaExtended method
     */
    private Stream<String> findDiscoverSchemaExtendedErrors(final AnnotationFinder finder) {

        final Stream<String> optionParameter = finder
                .findAnnotatedMethods(DiscoverSchemaExtended.class)
                .stream()
                .filter(m -> !hasOption(m))
                .map(m -> m + " should have a parameter being an option (marked with @Option)")
                .sorted();

        final Stream<String> returnType = finder
                .findAnnotatedMethods(DiscoverSchemaExtended.class)
                .stream()
                .filter(m -> !hasCorrectReturnType(m))
                .map(m -> m + " should return a Schema assignable")
                .sorted();

        final Stream<String> incomingSchema = finder
                .findAnnotatedMethods(DiscoverSchemaExtended.class)
                .stream()
                .filter(m -> hasTypeParameter(m, Schema.class))
                .filter(m -> !hasSchemaCorrectNaming(m))
                .map(m -> m + " should have its Schema `incomingSchema' parameter named `incomingSchema'")
                .sorted();

        final Stream<String> branch = finder
                .findAnnotatedMethods(DiscoverSchemaExtended.class)
                .stream()
                .filter(m -> hasTypeParameter(m, String.class))
                .filter(m -> !hasBranchCorrectNaming(m))
                .map(m -> m + " should have its String `branch' parameter named `branch'")
                .sorted();

        return Stream.of(returnType, optionParameter, incomingSchema, branch)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    private Stream<String> findUpdatesErrors(final AnnotationFinder finder) {
        final Map<String, Method> updates = finder
                .findAnnotatedMethods(Update.class)
                .stream()
                .collect(toMap(m -> m.getAnnotation(Update.class).value(), identity()));
        final Stream<String> updateAction = updates
                .values()
                .stream()
                .filter(m -> isPrimitiveLike(m.getReturnType()))
                .map(m -> m + " should return an object")
                .sorted();

        final List<Field> updatableFields = finder.findAnnotatedFields(Updatable.class);
        final Stream<String> directChild = updatableFields
                .stream()
                .filter(f -> f.getAnnotation(Updatable.class).after().contains(".") /* no '..' or '.' */)
                .map(f -> "@Updatable.after should only reference direct child primitive fields")
                .sorted();

        final Stream<String> noPrimitive = updatableFields
                .stream()
                .filter(f -> isPrimitiveLike(f.getType()))
                .map(f -> "@Updatable should not be used on primitives: " + f)
                .sorted();

        final Stream<String> serviceType = updatableFields.stream().map(f -> {
            final Method service = updates.get(f.getAnnotation(Updatable.class).value());
            if (service == null) {
                return null; // another error will mention it
            }
            if (f.getType().isAssignableFrom(service.getReturnType())) {
                return null; // no error
            }
            return "@Updatable field '" + f + "' does not match returned type of '" + service + "'";
        }).filter(Objects::nonNull).sorted();

        final Stream<String> noFieldUpdatable = updatableFields
                .stream()
                .filter(f -> updates.get(f.getAnnotation(Updatable.class).value()) == null)
                .map(f -> "No @Update service found for field " + f + ", did you intend to use @Updatable?")
                .sorted();
        return Stream
                .of(updateAction, directChild, noPrimitive, serviceType, noFieldUpdatable)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    private int countParameters(final Method m) {
        return countParameters(m.getParameters());
    }

    private int countParameters(final Parameter[] params) {
        return (int) Stream.of(params).filter(p -> !this.helper.isService(p)).count();
    }

    private boolean isPrimitiveLike(final Class<?> type) {
        return type.isPrimitive() || type == String.class;
    }

    private boolean hasOption(final Method method) {
        return Arrays.stream(method.getParameters())
                .filter(p -> p.isAnnotationPresent(Option.class))
                .count() == 1;
    }

    private boolean hasTypeParameter(final Method method, final Class<?> clazz) {
        return Arrays.stream(method.getParameters())
                .filter(p -> clazz.isAssignableFrom(p.getType()))
                .count() == 1;
    }

    private boolean hasSchemaCorrectNaming(final Method method) {
        return Arrays.stream(method.getParameters())
                .filter(p -> Schema.class.isAssignableFrom(p.getType()))
                .filter(p -> "incomingSchema".equals(p.getName()))
                .count() == 1;
    }

    private boolean hasBranchCorrectNaming(final Method method) {
        return Arrays.stream(method.getParameters())
                .filter(p -> String.class.isAssignableFrom(p.getType()))
                .filter(p -> "branch".equals(p.getName()))
                .count() == 1;
    }

    private boolean hasCorrectReturnType(final Method method) {
        return Schema.class.isAssignableFrom(method.getReturnType());
    }
}
