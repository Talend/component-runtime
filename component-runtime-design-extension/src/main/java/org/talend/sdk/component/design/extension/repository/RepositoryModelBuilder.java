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

import static java.util.stream.Collectors.toList;

/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class RepositoryModelBuilder {

    public RepositoryModel create(Collection<ComponentFamilyMeta> FamiliesMeta) {
        RepositoryModel repositoryModel = new RepositoryModel(new ArrayList<>());
        FamiliesMeta.stream().forEach((ComponentFamilyMeta fMeta) -> {//foreach family metadata
            Family family = new Family();
            family.setName(fMeta.getName());
            family.setDisplayName(fMeta.getName());
            family.setIcon(family.getIcon());
            repositoryModel.getFamilies().add(family);

            //Unique Configuration parameters by family
            Set<ParameterMeta> allFlatMetas = new HashSet<>();
            Map<ConfigKey, ParameterMeta> configurations = new HashMap<>();
            Stream.concat(fMeta.getPartitionMappers().values().stream(), fMeta.getProcessors().values().stream())
                  .forEach(cMeta ->
                          cMeta.getParameterMetas().stream()
                               .filter(RepositoryModelBuilder::isConfiguration)
                               .forEach(meta -> addConfiguration(family.getName(), meta, configurations)
                               ));

            configurations.values().forEach(c -> addParameterMeta(c, allFlatMetas));
            Set<ParameterMeta> nestedToIgnore = new HashSet<>();
            Set<ParameterMeta> configMetaWithoutNP = new HashSet<>(); // config meta without nested params
            while (!allFlatMetas.isEmpty()) {
                configMetaWithoutNP.clear();
                allFlatMetas.forEach(meta -> {
                    addIfHasNoNestedConfig(meta, configMetaWithoutNP, nestedToIgnore);
                });
                nestedToIgnore.addAll(configMetaWithoutNP);

                if (family.getConfigs().isEmpty()) {//first root elements
                    family.getConfigs().addAll(configMetaWithoutNP
                            .stream()
                            .map(config -> createConfig(config, family.getName(), family.getIcon()))
                            .collect(toList()));
                } else {
                    configMetaWithoutNP.forEach(meta -> {
                        addNode(meta, family.getConfigs(), family.getName(), family.getIcon());
                    });
                }
                allFlatMetas.removeAll(configMetaWithoutNP);

                //if no more nested meta create props in nodes
                if (configMetaWithoutNP.isEmpty() && !allFlatMetas.isEmpty()) {
                    allFlatMetas.forEach(prop -> {
                        addProp(prop, family.getConfigs(), configurations);
                    });
                    allFlatMetas.clear();
                }
            }
        });

        return repositoryModel;
    }

    private void addConfiguration(String familyName, ParameterMeta meta, Map<ConfigKey, ParameterMeta> configurations) {
        configurations
                .computeIfAbsent(getKey(familyName, meta.getMetadata()), s -> meta);

        if (meta.getNestedParameters() == null) {
            return;
        }

        meta.getNestedParameters().stream()
            .filter(RepositoryModelBuilder::isConfiguration)
            .forEach(np -> addConfiguration(familyName, np, configurations));
    }

    private void addProp(ParameterMeta prop, List<Config> configs,
            Map<ConfigKey, ParameterMeta> configurations) {
        configs.forEach(config -> {
            if (configurations.get(config.getKey()).getNestedParameters() != null
                    && configurations.get(config.getKey()).getNestedParameters().contains(prop)) {
                config.getProperties().add(prop);
            } else {
                addProp(prop, config.getChildConfigs(), configurations);
            }
        });
    }

    private void addNode(ParameterMeta meta, List<Config> configs, String familyName, String familyIcon) {
        configs.forEach(config -> {
            if (config.getPath().startsWith(meta.getPath())
                    && meta.getNestedParameters().stream()
                           .filter(np -> getKey(familyName, np.getMetadata()).equals(config.getKey()))
                           .findFirst().isPresent()) {

                Config childConfig = createConfig(meta, familyName, familyIcon);
                childConfig.setParent(config);
                config.getChildConfigs().add(childConfig);
            } else {
                addNode(meta, config.getChildConfigs(), familyName, familyIcon);
            }
        });
    }

    private Config createConfig(ParameterMeta config, String familyName, String familyIcon) {
        Config c = new Config();
        c.setName(config.getName());
        c.setDisplayName(config.getName()); //todo how to get that
        c.setIcon(familyIcon);
        c.setPath(config.getPath());
        c.setKey(getKey(familyName, config.getMetadata()));
        return c;
    }

    private void addParameterMeta(ParameterMeta meta, Set<ParameterMeta> metas) {
        metas.add(meta);
        if (meta.getNestedParameters() == null) {
            return;
        }
        meta.getNestedParameters().forEach(nestedConfig -> addParameterMeta(nestedConfig, metas));
    }

    private void addIfHasNoNestedConfig(ParameterMeta meta, Set<ParameterMeta> result, Set<ParameterMeta> ignoreNested) {
        if (isConfiguration(meta) && !hasNestedConfig(meta, ignoreNested)) {
            result.add(meta);
            return;
        }

        meta.getNestedParameters().forEach(nestedParam -> {
            addIfHasNoNestedConfig(nestedParam, result, ignoreNested);
        });
    }

    private boolean hasNestedConfig(ParameterMeta parameterMeta, Set<ParameterMeta> ignoreNested) {

        if (parameterMeta.getNestedParameters() == null) {
            return false;
        }

        return parameterMeta.getNestedParameters().stream()
                            .filter(RepositoryModelBuilder::isConfiguration)
                            .filter(np -> !ignoreNested.contains(np))
                            .findFirst().isPresent();
    }

    /**
     * @param meta
     * @return unique key for a Configuration parameter
     */
    private ConfigKey getKey(String family, Map<String, String> meta) {
        return new ConfigKey(family, meta.get("tcomp::configurationtype::type"),
                meta.get("tcomp::configurationtype::name"));
    }

    private static boolean isConfiguration(ParameterMeta parameterMeta) {
        return parameterMeta.getMetadata()
                            .entrySet()
                            .stream()
                            .filter(m -> m.getKey().startsWith("tcomp::configurationtype::"))
                            .findFirst().isPresent();
    }

}
