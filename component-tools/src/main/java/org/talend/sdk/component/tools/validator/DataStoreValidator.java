package org.talend.sdk.component.tools.validator;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.action.Checkable;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class DataStoreValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public DataStoreValidator(ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(AnnotationFinder finder, List<Class<?>> components) {
        List<String> errors = new ArrayList<>();
        final List<Class<?>> datastoreClasses = finder.findAnnotatedClasses(DataStore.class);

        final List<String> datastores =
                datastoreClasses.stream().map(d -> d.getAnnotation(DataStore.class).value()).collect(toList());

        Set<String> uniqueDatastores = new HashSet<>(datastores);
        if (datastores.size() != uniqueDatastores.size()) {
            errors
                    .add("Duplicated DataStore found : " + datastores
                            .stream()
                            .collect(groupingBy(identity()))
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().size() > 1)
                            .map(Map.Entry::getKey)
                            .collect(joining(", ")));
        }

        final List<Class<?>> checkableClasses = finder.findAnnotatedClasses(Checkable.class);
        errors
                .addAll(checkableClasses
                        .stream()
                        .filter(d -> !d.isAnnotationPresent(DataStore.class))
                        .map(c -> c.getName() + " has @Checkable but is not a @DataStore")
                        .sorted()
                        .collect(Collectors.toList()));

        final Map<String, String> checkableDataStoresMap = checkableClasses
                .stream()
                .filter(d -> d.isAnnotationPresent(DataStore.class))
                .collect(toMap(d -> d.getAnnotation(DataStore.class).value(),
                        d -> d.getAnnotation(Checkable.class).value()));

        final Set<String> healthchecks = finder
                .findAnnotatedMethods(HealthCheck.class)
                .stream()
                .filter(h -> h.getDeclaringClass().isAnnotationPresent(Service.class))
                .map(m -> m.getAnnotation(HealthCheck.class).value())
                .collect(Collectors.toSet());
        errors
                .addAll(checkableDataStoresMap
                        .entrySet()
                        .stream()
                        .filter(e -> !healthchecks.contains(e.getValue()))
                        .map(e -> "No @HealthCheck for dataStore: '" + e.getKey() + "' with checkable: '" + e.getValue()
                                + "'")
                        .sorted()
                        .collect(Collectors.toList()));

        errors
                .addAll(datastoreClasses
                        .stream()
                        .map(clazz -> this.helper.validateFamilyI18nKey(clazz,
                                "${family}.datastore." + clazz.getAnnotation(DataStore.class).value()
                                        + "._displayName"))
                        .filter(Objects::nonNull)
                        .collect(toList()));

        return errors.stream();

    }
}
