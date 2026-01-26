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
package org.talend.sdk.component.test.connectors.service;

import java.util.Arrays;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.discovery.DiscoverDataset;
import org.talend.sdk.component.api.service.discovery.DiscoverDatasetResult;

@Service
public class DiscoverDatasetServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - Discoverdataset https://talend.github.io/component-runtime/main/latest/ref-actions.html#_discoverdataset
     *
     */

    public final static String DISCOVER_DATASET = "action_DISCOVER_DATASET";

    /**
     * DiscoverDataset action
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/ref-actions.html#_discoverdataset
     * Type: discoverdataset
     * API: @org.talend.sdk.component.api.service.discovery.DiscoverDataset
     *
     * Returned type: org.talend.sdk.component.api.service.discovery.DiscoverDatasetResult
     */

    @DataStore
    public static class FakeDataStore {

    }

    @DiscoverDataset(DISCOVER_DATASET)
    public DiscoverDatasetResult discoverDataset(@Option final FakeDataStore dataStore) {
        final DiscoverDatasetResult.DatasetDescription datasetA =
                new DiscoverDatasetResult.DatasetDescription("datasetA");
        datasetA.addMetadata("type", "typeA");
        final DiscoverDatasetResult.DatasetDescription datasetB =
                new DiscoverDatasetResult.DatasetDescription("datasetB");
        datasetB.addMetadata("type", "typeB");
        return new DiscoverDatasetResult(Arrays.asList(datasetA, datasetB));
    }

}
