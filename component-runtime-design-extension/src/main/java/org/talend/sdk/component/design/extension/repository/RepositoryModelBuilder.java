package org.talend.sdk.component.design.extension.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;

import static java.util.stream.Collectors.toList;

/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

    public static final String CONFIG_TYPE_TYPE = "tcomp::configurationtype::type";

    public static final String CONFIG_TYPE_NAME = "tcomp::configurationtype::name";

    public RepositoryModel create(final Collection<ComponentFamilyMeta> familyMetas) {
        final RepositoryModel repositoryModel = new RepositoryModel(new ArrayList<>());
        familyMetas.forEach((ComponentFamilyMeta familyMeta) -> {// foreach family metadata
            final Family family = new Family();
            family.setId(IdGenerator.get(familyMeta.getName()));
            family.setMeta(familyMeta);

            // Unique Configuration parameters by family
            final Set<ParameterMeta> allFlatMetas = new HashSet<>();
            final Map<ConfigKey, ParameterMeta> configurations = new HashMap<>();
            Stream
                .concat(familyMeta.getPartitionMappers().values().stream(),
                    familyMeta.getProcessors().values().stream())
                .forEach(cMeta -> cMeta.getParameterMetas().stream().filter(RepositoryModelBuilder::isConfiguration)
                    .forEach(meta -> addConfiguration(family.getMeta().getName(), meta, configurations)));

            configurations.values().forEach(c -> addParameterMeta(c, allFlatMetas));
            final Set<ParameterMeta> nestedToIgnore = new HashSet<>();
            final Set<ParameterMeta> configMetaWithoutNP = new HashSet<>(); // config meta without nested params
            while (!allFlatMetas.isEmpty()) {
                configMetaWithoutNP.clear();
                allFlatMetas.forEach(meta -> {
                    addIfHasNoNestedConfig(meta, configMetaWithoutNP, nestedToIgnore);
                });
                nestedToIgnore.addAll(configMetaWithoutNP);

                if (family.getConfigs().isEmpty()) {// first root elements
                    family.getConfigs()
                        .addAll(configMetaWithoutNP.stream()
                            .map(config -> createConfig(config, family.getMeta().getName(), family.getMeta().getIcon()))
                            .collect(toList()));
                } else {
                    configMetaWithoutNP.forEach(meta -> {
                        addNode(meta, family.getConfigs(), family.getMeta().getName(), family.getMeta().getIcon());
                    });
                }
                allFlatMetas.removeAll(configMetaWithoutNP);

                // if no more nested meta create props in nodes
                if (configMetaWithoutNP.isEmpty() && !allFlatMetas.isEmpty()) {
                    allFlatMetas.forEach(prop -> {
                        addProp(prop, family.getConfigs(), configurations);
                    });
                    allFlatMetas.clear();
                }
            }

            if (family.getConfigs().isEmpty()) {
                repositoryModel.getFamilies().add(family);
            }
        });

        return repositoryModel;
    }

    private void addConfiguration(final String familyName, final ParameterMeta meta,
        final Map<ConfigKey, ParameterMeta> configurations) {
        configurations.putIfAbsent(getKey(familyName, meta.getMetadata()), meta);

        if (meta.getNestedParameters() == null) {
            return;
        }

        meta.getNestedParameters().stream().filter(RepositoryModelBuilder::isConfiguration)
            .forEach(np -> addConfiguration(familyName, np, configurations));
    }

    private void addProp(final ParameterMeta prop, final List<Config> configs,
        final Map<ConfigKey, ParameterMeta> configurations) {
        configs.forEach(config -> {
            if (configurations.get(config.getKey()).getNestedParameters() != null
                && configurations.get(config.getKey()).getNestedParameters().contains(prop)) {
                config.getProperties().add(prop);
            } else {
                addProp(prop, config.getChildConfigs(), configurations);
            }
        });
    }

    private void addNode(final ParameterMeta meta, final List<Config> configs, final String familyName,
        final String familyIcon) {
        configs.forEach(config -> {
            if (config.getMeta().getPath().startsWith(meta.getPath()) && meta.getNestedParameters().stream()
                .anyMatch(np -> getKey(familyName, np.getMetadata()).equals(config.getKey()))) {

                Config childConfig = createConfig(meta, familyName, familyIcon);
                childConfig.setParent(config);
                config.getChildConfigs().add(childConfig);
            } else {
                addNode(meta, config.getChildConfigs(), familyName, familyIcon);
            }
        });
    }

    private Config createConfig(final ParameterMeta config, final String familyName, final String familyIcon) {
        final Config c = new Config();
        c.setIcon(familyIcon);
        c.setKey(getKey(familyName, config.getMetadata()));
        c.setMeta(config);
        c.setId(IdGenerator.get(c.getKey().getFamily(), c.getKey().getConfigType(), c.getKey().getConfigName()));
        return c;
    }

    private void addParameterMeta(final ParameterMeta meta, final Set<ParameterMeta> metas) {
        metas.add(meta);
        if (meta.getNestedParameters() == null) {
            return;
        }
        meta.getNestedParameters().forEach(nestedConfig -> addParameterMeta(nestedConfig, metas));
    }

    private void addIfHasNoNestedConfig(final ParameterMeta meta, final Set<ParameterMeta> result,
        final Set<ParameterMeta> ignoreNested) {
        if (isConfiguration(meta) && !hasNestedConfig(meta, ignoreNested)) {
            result.add(meta);
            return;
        }

        meta.getNestedParameters().forEach(nestedParam -> addIfHasNoNestedConfig(nestedParam, result, ignoreNested));
    }

    private boolean hasNestedConfig(final ParameterMeta parameterMeta, final Set<ParameterMeta> ignoreNested) {

        return parameterMeta.getNestedParameters() != null && parameterMeta.getNestedParameters().stream()
            .filter(RepositoryModelBuilder::isConfiguration).anyMatch(np -> !ignoreNested.contains(np));

    }

    private ConfigKey getKey(final String family, final Map<String, String> meta) {
        return new ConfigKey(family, meta.get(CONFIG_TYPE_NAME), meta.get(CONFIG_TYPE_TYPE));
    }

    private static boolean isConfiguration(final ParameterMeta parameterMeta) {
        return parameterMeta.getMetadata().entrySet().stream()
            .anyMatch(m -> m.getKey().startsWith("tcomp::configurationtype::"));
    }

}
