package org.talend.sdk.component.design.extension.repository;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;

/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
public class RepositoryModelBuilder {

    public RepositoryModel create(final ComponentManager.AllServices services,
            final Collection<ComponentFamilyMeta> familyMetas) {
        return new RepositoryModel(familyMetas
                .stream()
                .map(familyMeta -> Stream
                        .concat(familyMeta.getPartitionMappers().values().stream(),
                                familyMeta.getProcessors().values().stream())
                        .flatMap(b -> b.getParameterMetas().stream())
                        .filter(RepositoryModelBuilder::isConfiguration)
                        .flatMap(this::toConfiguration)
                        .map(p -> createConfig(services, p, familyMeta.getName(), familyMeta.getIcon()))
                        .collect(toMap(c -> c.getMeta().getJavaType(), identity(), (config1, config2) -> config1))
                        .values()
                        .stream()
                        .sorted((o1, o2) -> {
                            if (toParamStream(o1.getMeta().getNestedParameters())
                                    .anyMatch(p -> p.getJavaType() == o2.getMeta().getJavaType())) {
                                return 1;
                            }
                            if (toParamStream(o2.getMeta().getNestedParameters())
                                    .anyMatch(p -> p.getJavaType() == o1.getMeta().getJavaType())) {
                                return -1;
                            }
                            return o1.getMeta().getPath().compareTo(o2.getMeta().getPath());
                        })
                        .collect(() -> {
                            final Family family = new Family();
                            family.setId(IdGenerator.get(familyMeta.getName()));
                            family.setMeta(familyMeta);
                            return family;
                        }, (aggregator, item) -> {
                            final Collection<Config> configs = aggregator
                                    .getConfigs()
                                    .stream()
                                    .filter(c -> toParamStream(item.getMeta().getNestedParameters())
                                            .anyMatch(p -> p.getJavaType() == c.getMeta().getJavaType()))
                                    .findFirst()
                                    .map(Config::getChildConfigs)
                                    .orElseGet(aggregator::getConfigs);
                            if (configs.stream().noneMatch(
                                    c -> c.getMeta().getJavaType() == item.getMeta().getJavaType())) {
                                configs.add(item);
                            }
                        }, (family1, family2) -> family1.getConfigs().addAll(family2.getConfigs())))
                .collect(toList()));
    }

    private Stream<ParameterMeta> toParamStream(final Collection<ParameterMeta> params) {
        if (params.isEmpty()) {
            return Stream.empty();
        }
        return Stream.concat(params.stream(), params.stream().flatMap(p -> toParamStream(p.getNestedParameters())));
    }

    private Stream<ParameterMeta> toConfiguration(final ParameterMeta meta) {
        if (meta.getNestedParameters() == null) {
            return Stream.of(meta);
        }
        return Stream.concat(Stream.of(meta),
                meta.getNestedParameters().stream().filter(RepositoryModelBuilder::isConfiguration).flatMap(
                        this::toConfiguration));
    }

    private Config createConfig(final ComponentManager.AllServices services, final ParameterMeta config,
            final String familyName, final String familyIcon) {
        final Config c = new Config();
        c.setIcon(familyIcon);
        c.setKey(getKey(familyName, config.getMetadata()));
        c.setMeta(config);
        c.setId(IdGenerator.get(c.getKey().getFamily(), c.getKey().getConfigType(), c.getKey().getConfigName()));

        if (Class.class.isInstance(config.getJavaType())) {
            final Class<?> clazz = Class.class.cast(config.getJavaType());
            final Version version = clazz.getAnnotation(Version.class);
            if (version != null) {
                c.setVersion(version.value());
                if (version.migrationHandler() != MigrationHandler.class) {
                    // ComponentManager already created it
                    // otherwise it is a dead config type and we don't care to migrate it
                    c.setMigrationHandler(MigrationHandler.class.cast(services.getServices().get(clazz)));
                }
            } else {
                c.setVersion(-1);
                c.setMigrationHandler((v, d) -> d);
            }
        }

        return c;
    }

    private ConfigKey getKey(final String family, final Map<String, String> meta) {
        final String configName = meta.get("tcomp::configurationtype::name");
        final String configType = meta.get("tcomp::configurationtype::type");
        return new ConfigKey(family, configName, configType);
    }

    private static boolean isConfiguration(final ParameterMeta parameterMeta) {
        return parameterMeta.getMetadata().keySet().stream().anyMatch(m -> m.startsWith("tcomp::configurationtype::"));
    }

}
