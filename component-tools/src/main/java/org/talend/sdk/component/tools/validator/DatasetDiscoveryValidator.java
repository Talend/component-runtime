/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.type.DatasetDiscovery;
import org.talend.sdk.component.api.configuration.type.DatasetDiscoveryConfiguration;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;

public class DatasetDiscoveryValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public DatasetDiscoveryValidator(final Validators.ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final List<Class<?>> datasetDiscoveryClasses = finder.findAnnotatedClasses(DatasetDiscovery.class);
        final Map<Class<?>, String> datasetDiscoveries = datasetDiscoveryClasses
                .stream()
                .collect(toMap(identity(), d -> d.getAnnotation(DatasetDiscovery.class).value()));

        final Stream<String> duplicated = DatasetValidator.duplicatedDataset(datasetDiscoveries.values());

        final Stream<String> i18nError = datasetDiscoveries
                .entrySet()
                .stream()
                .map(entry -> this.helper
                        .validateFamilyI18nKey(entry.getKey(),
                                "${family}.datasetdiscovery." + entry.getValue() + "._displayName"))
                .filter(Objects::nonNull);

        // "cloud" rule - ensure all datasetDiscoveries have a datastore
        final BaseParameterEnricher.Context context =
                new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "tools"));
        final Stream<String> withoutStore = datasetDiscoveryClasses
                .stream()
                .map((Class<?> ds) -> this.findDatasetDiscoveryWithoutDataStore(ds, context))
                .filter(Objects::nonNull)
                .sorted();

        // A datasetDiscovery must implement interface DatasetDiscoveryConfiguration
        final Stream<String> implementationError = datasetDiscoveryClasses
                .stream()
                .filter((Class<?> ds) -> !(DatasetDiscoveryConfiguration.class.isAssignableFrom(ds)))
                .map((Class<?> ds) -> "Class " + ds.getName() + " must implement "
                        + DatasetDiscoveryConfiguration.class)
                .sorted();

        return Stream
                .of(duplicated, i18nError, withoutStore, implementationError)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    private String findDatasetDiscoveryWithoutDataStore(final Class<?> ds,
            final BaseParameterEnricher.Context context) {
        final List<ParameterMeta> dataset = helper
                .getParameterModelService()
                .buildParameterMetas(
                        Stream.of(new ParameterModelService.Param(ds, ds.getAnnotations(), "datasetDiscovery")), ds,
                        ofNullable(ds.getPackage()).map(Package::getName).orElse(""), true, context);
        if (DatasetValidator
                .flatten(dataset)
                .noneMatch(prop -> "datastore".equals(prop.getMetadata().get("tcomp::configurationtype::type")))) {
            return "The datasetDiscovery " + ds.getName()
                    + " is missing a datastore reference in its configuration (see @DataStore)";
        }
        return null;
    }
}
