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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withdatastore.service;

import java.util.List;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.junit5.WithComponents;
import org.talend.sdk.component.sample.feature.dynamicdependencies.AbstractDynamicDependenciesServiceTest;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withdatastore.config.Config;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withdatastore.config.Dataset;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withdatastore.config.Datastore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithComponents(value = "org.talend.sdk.component.sample.feature.dynamicdependencies.withdatastore")
public class DynamicDependenciesWithDatastoreServiceTest
        extends AbstractDynamicDependenciesServiceTest<Config, DynamicDependenciesWithDatastoreService> {

    @Service
    DynamicDependenciesWithDatastoreService dynamicDependenciesServiceService;

    @Override
    protected Config buildConfig() {
        Config config = new Config();
        Dataset dse = new Dataset();
        Datastore dso = new Datastore();
        List<Dependency> depends = this.getDependList();
        dso.setDependencies(depends);
        dse.setDso(dso);
        config.setDse(dse);
        config.setEnvironmentInformation(true);

        return config;
    }

    @Override
    protected DynamicDependenciesWithDatastoreService getService() {
        return dynamicDependenciesServiceService;
    }
}