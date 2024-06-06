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
package org.talend.sdk.component.design.extension.repository;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.runtime.manager.util.Lazy.lazy;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.MigrationHandlerFactory;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;

/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
            final Collection<ComponentFamilyMeta> familyMetas, final MigrationHandlerFactory migrationHandlerFactory) {
        final List<Family> families = familyMetas
                .stream()
                .map(familyMeta -> createConfigForFamily(services, migrationHandlerFactory, familyMeta))
                .collect(toList());
        return new RepositoryModel(families);
    }

    private Family createConfigForFamily(final ComponentManager.AllServices services,
            final MigrationHandlerFactory migrationHandlerFactory, final ComponentFamilyMeta familyMeta) {
        final Family family = new Family();
        family.setId(familyMeta.getId());
        family.setMeta(familyMeta);
        family
                .setConfigs(lazy(() -> extractConfigurations(services, migrationHandlerFactory, familyMeta)
                        .values()
                        .stream()
                        .collect(ArrayList::new, (aggregator, item) -> {
                            final Collection<Config> configs = aggregator
                                    .stream()
                                    .filter(c -> toParamStream(item.getMeta().getNestedParameters())
                                            .anyMatch(p -> p.getJavaType() == c.getMeta().getJavaType()))
                                    .findFirst()
                                    .map(Config::getChildConfigs)
                                    .orElse(aggregator);
                            if (configs
                                    .stream()
                                    .noneMatch(c -> c.getMeta().getJavaType() == item.getMeta().getJavaType())) {
                                configs.add(item);
                            }
                        }, List::addAll)));
        return family;
    }

    private Map<Type, Config> extractConfigurations(final ComponentManager.AllServices services,
            final MigrationHandlerFactory migrationHandlerFactory, final ComponentFamilyMeta familyMeta) {
        return Stream
                .of(familyMeta.getPartitionMappers().values().stream(), familyMeta.getProcessors().values().stream(),
                        familyMeta.getDriverRunners().values().stream())
                .flatMap(t -> t)
                .flatMap(b -> b.getParameterMetas().get().stream())
                .flatMap(this::flatten)
                .filter(RepositoryModelBuilder::isConfiguration)
                .map(p -> createConfig(familyMeta.getPlugin(), services, p, familyMeta.getName(), familyMeta.getIcon(),
                        migrationHandlerFactory))
                .collect(toMap(c -> c.getMeta().getJavaType(), identity(), (config1, config2) -> config1,
                        LinkedHashMap::new));
    }

    private Stream<ParameterMeta> toParamStream(final Collection<ParameterMeta> params) {
        if (params.isEmpty()) {
            return Stream.empty();
        }
        return Stream.concat(params.stream(), params.stream().flatMap(p -> toParamStream(p.getNestedParameters())));
    }

    private Stream<ParameterMeta> flatten(final ParameterMeta meta) {
        if (meta.getNestedParameters() == null || meta.getNestedParameters().isEmpty()) {
            return Stream.of(meta);
        }
        return Stream.concat(meta.getNestedParameters().stream().flatMap(this::flatten), Stream.of(meta));
    }

    private Config createConfig(final String plugin, final ComponentManager.AllServices services,
            final ParameterMeta config, final String familyName, final String familyIcon,
            final MigrationHandlerFactory migrationHandlerFactory) {
        final Config c = new Config();
        c.setIcon(familyIcon);
        c.setKey(getKey(familyName, config.getMetadata()));
        c.setMeta(translate(config, config.getPath().length(), "configuration"));
        c
                .setId(IdGenerator
                        .get(plugin, c.getKey().getFamily(), c.getKey().getConfigType(), c.getKey().getConfigName()));

        if (Class.class.isInstance(config.getJavaType())) {
            final Class<?> clazz = Class.class.cast(config.getJavaType());
            final Version version = clazz.getAnnotation(Version.class);
            if (version != null) {
                c.setVersion(version.value());
                if (version.migrationHandler() != MigrationHandler.class) {
                    c
                            .setMigrationHandler(migrationHandlerFactory
                                    .findMigrationHandler(() -> singletonList(c.getMeta()), clazz, services));
                }
            } else {
                c.setVersion(1);
            }
        }
        if (c.getMigrationHandler() == null) {
            c.setMigrationHandler((v, d) -> d);
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

    private ParameterMeta translate(final ParameterMeta config, final int replacedPrefixLen, final String newPrefix) {
        return new ParameterMeta(config.getSource(), config.getJavaType(), config.getType(),
                newPrefix + config.getPath().substring(replacedPrefixLen),
                config.getPath().length() == replacedPrefixLen ? newPrefix : config.getName(), config.getI18nPackages(),
                config
                        .getNestedParameters()
                        .stream()
                        .map(it -> translate(it, replacedPrefixLen, newPrefix))
                        .collect(toList()),
                config.getProposals(), config.getMetadata(), config.isLogMissingResourceBundle());
    }
}
