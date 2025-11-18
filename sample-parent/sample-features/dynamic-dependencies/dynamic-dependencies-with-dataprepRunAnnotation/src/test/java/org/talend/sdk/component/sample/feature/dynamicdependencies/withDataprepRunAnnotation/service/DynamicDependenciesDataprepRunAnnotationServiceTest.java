/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.service;

import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.sample.feature.dynamicdependencies.AbstractDynamicDependenciesServiceTest;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.config.Config;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.config.Dataset;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.config.Datastore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithComponents(value = "org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation")
public class DynamicDependenciesDataprepRunAnnotationServiceTest
        extends AbstractDynamicDependenciesServiceTest<Config, DynamicDependenciesDataprepRunAnnotationService> {

    @Service
    DynamicDependenciesDataprepRunAnnotationService dynamicDependenciesServiceService;

    @Override
    protected Config buildConfig() {
        Config config = new Config();
        Dataset dse = new Dataset();
        Datastore dso = new Datastore();
        List<Dependency> depends = this.getDependList();
        config.getSubConfig().setDependencies(depends);
        dse.setDso(dso);
        config.setDse(dse);
        config.setEnvironmentInformation(true);

        return config;
    }

    // use tck cnnector as dependency
    protected List<Dependency> getDependList() {
        List<Dependency> depends = new ArrayList<>();
        Dependency depend = new Dependency();
        depend.setArtifactId("commons-numbers-primes");
        depend.setVersion("1.2");
        depend.setGroupId("org.apache.commons");
        depend.setClazz("org.apache.commons.numbers.primes.SmallPrimes");
        depends.add(depend);

        // //for connector depend
        // Dependency depend2 = new Dependency();
        // depend.setArtifactId("record-provider");
        // depend.setVersion("1.71.0-SNAPSHOT");
        // depend.setGroupId("org.talend.components");
        // depend.setClazz("org.talend.components.recordprovider.source.GenericMapper");
        // depends.add(depend2);
        return depends;
    }

    @Override
    protected DynamicDependenciesDataprepRunAnnotationService getService() {
        return dynamicDependenciesServiceService;
    }
}