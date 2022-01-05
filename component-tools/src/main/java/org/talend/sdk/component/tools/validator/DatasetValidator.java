/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class DatasetValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public DatasetValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final List<Class<?>> datasetClasses = finder.findAnnotatedClasses(DataSet.class);
        final Map<Class<?>, String> datasets =
                datasetClasses.stream().collect(toMap(identity(), d -> d.getAnnotation(DataSet.class).value()));

        final Stream<String> duplicated = this.duplicatedDataset(datasets.values());

        final Stream<String> i18nError = datasets
                .entrySet()
                .stream()
                .map(entry -> this.helper
                        .validateFamilyI18nKey(entry.getKey(),
                                "${family}.dataset." + entry.getValue() + "._displayName"))
                .filter(Objects::nonNull);

        // ensure there is always a source with a config matching without user entries each dataset
        final Map<Class<?>, Collection<ParameterMeta>> componentNeedingADataSet = components
                .stream()
                .filter(c -> isSource(c) || isOutput(c))
                .collect(toMap(identity(), helper::buildOrGetParameters));

        final Map<? extends Class<?>, Collection<ParameterMeta>> inputs = componentNeedingADataSet
                .entrySet()
                .stream()
                .filter(it -> isSource(it.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Stream<String> sourceWithoutDataset = datasets
                .entrySet()
                .stream()
                .filter(dataset -> inputs.isEmpty() || inputs.entrySet().stream().allMatch(input -> {
                    final Collection<ParameterMeta> allProps = flatten(input.getValue()).collect(toList());
                    final Collection<ParameterMeta> datasetProperties =
                            findNestedDataSets(allProps, dataset.getValue()).collect(toList());
                    return !datasetProperties.isEmpty() && allProps
                            .stream()
                            // .filter(it -> it.getType() != OBJECT && it.getType() != ARRAY) // should it be
                            // done?
                            .filter(it -> datasetProperties
                                    .stream()
                                    .noneMatch(dit -> it.getPath().equals(dit.getPath())
                                            || it.getPath().startsWith(dit.getPath() + '.')))
                            .anyMatch(this::isRequired);
                }))
                .map(dataset -> "No source instantiable without adding parameters for @DataSet(\"" + dataset.getValue()
                        + "\") (" + dataset.getKey().getName() + "), please ensure at least a source using this "
                        + "dataset can be used just filling the dataset information.")
                .sorted();

        // "cloud" rule - ensure all input/output have a dataset at least
        final Stream<String> configWithoutDataset = componentNeedingADataSet
                .entrySet()
                .stream()
                .filter(it -> flatten(it.getValue())
                        .noneMatch((ParameterMeta prop) -> "dataset"
                                .equals(prop.getMetadata().get("tcomp::configurationtype::type"))
                                || "datasetDiscovery".equals(prop.getMetadata().get("tcomp::configurationtype::type"))))
                .map(it -> "The component " + it.getKey().getName()
                        + " is missing a dataset in its configuration (see @DataSet)")
                .sorted();

        // "cloud" rule - ensure all datasets have a datastore
        final BaseParameterEnricher.Context context =
                new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools"));
        final Stream<String> withoutStore = datasetClasses
                .stream()
                .map((Class<?> ds) -> this.findDatasetWithoutDataStore(ds, context))
                .filter(Objects::nonNull)
                .sorted();
        return Stream
                .of(duplicated, i18nError, sourceWithoutDataset, configWithoutDataset, withoutStore)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    private String findDatasetWithoutDataStore(final Class<?> ds, final BaseParameterEnricher.Context context) {
        final List<ParameterMeta> dataset = helper
                .getParameterModelService()
                .buildParameterMetas(Stream.of(new ParameterModelService.Param(ds, ds.getAnnotations(), "dataset")), ds,
                        ofNullable(ds.getPackage()).map(Package::getName).orElse(""), true, context);
        if (flatten(dataset)
                .noneMatch(prop -> "datastore".equals(prop.getMetadata().get("tcomp::configurationtype::type")))) {
            return "The dataset " + ds.getName()
                    + " is missing a datastore reference in its configuration (see @DataStore)";
        }
        return null;
    }

    protected static Stream<String> duplicatedDataset(final Collection<String> datasets) {

        final Set<String> uniqueDatasets = new HashSet<>(datasets);
        if (datasets.size() != uniqueDatasets.size()) {
            return Stream
                    .of("Duplicated DataSet found : " + datasets
                            .stream()
                            .collect(Collectors.groupingBy(identity()))
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().size() > 1)
                            .map(Map.Entry::getKey)
                            .collect(joining(", ")));
        }
        return Stream.empty();
    }

    protected static Stream<ParameterMeta> flatten(final Collection<ParameterMeta> options) {
        return options
                .stream()
                .flatMap(it -> Stream
                        .concat(Stream.of(it),
                                it.getNestedParameters().isEmpty() ? empty() : flatten(it.getNestedParameters())));
    }

    private boolean isSource(final Class<?> component) {
        return component.isAnnotationPresent(PartitionMapper.class) || component.isAnnotationPresent(Emitter.class);
    }

    private boolean isOutput(final Class<?> component) {
        return component.isAnnotationPresent(Processor.class) && Stream
                .of(component.getMethods())
                .filter(it -> it.isAnnotationPresent(ElementListener.class) || it.isAnnotationPresent(AfterGroup.class))
                .allMatch(it -> void.class == it.getReturnType()
                        && Stream.of(it.getParameters()).noneMatch(param -> param.isAnnotationPresent(Output.class)));
    }

    private Stream<ParameterMeta> findNestedDataSets(final Collection<ParameterMeta> options, final String name) {
        return options
                .stream()
                .filter(it -> "dataset".equals(it.getMetadata().get("tcomp::configurationtype::type"))
                        && name.equals(it.getMetadata().get("tcomp::configurationtype::name")));
    }

    private boolean isRequired(final ParameterMeta parameterMeta) {
        return Boolean.parseBoolean(parameterMeta.getMetadata().getOrDefault("tcomp::validation::required", "false"));
    }

}
