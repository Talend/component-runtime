package org.talend.sdk.component.test.connectors.service;

import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.test.connectors.config.DynamicDependenciesConf;

@Service
public class DynamicDependenciesService {

    /**
     * This is a fake dynamic depencencies service.
     * It returns 3 times the dependency define in the configuration with version 1.0, 2.0 and 3.0.
     *
     * @param conf
     * @return The list of dynamic dependencies.
     */
    @DynamicDependencies
    public List<String> getDynamicDependencies(@Option("DynDepsConfig") DynamicDependenciesConf conf) {
        List<String> dependencies = new ArrayList<>();

        for (int version = 1; version <= 3; version++) {
            dependencies.add(String.format("%s:%s:%s.0", conf.getGroup(), conf.getArtifact(), version));
        }

        return dependencies;
    }

}