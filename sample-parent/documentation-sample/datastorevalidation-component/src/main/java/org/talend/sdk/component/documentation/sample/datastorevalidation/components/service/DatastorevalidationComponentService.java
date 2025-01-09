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
package org.talend.sdk.component.documentation.sample.datastorevalidation.components.service;

import org.talend.sdk.component.documentation.sample.datastorevalidation.components.datastore.DatastoreA;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;

@Service
public class DatastorevalidationComponentService {

    // you can put logic here you can reuse in components
    @HealthCheck
    public HealthCheckStatus testConnection(DatastoreA datastoreA) {

        if (datastoreA == null || datastoreA.getUsername().equals("invalidtest")) {
            return new HealthCheckStatus(HealthCheckStatus.Status.KO, "Connection not ok, datastore can't be null");
        }
        return new HealthCheckStatus(HealthCheckStatus.Status.OK, "Connection ok");
    }

}