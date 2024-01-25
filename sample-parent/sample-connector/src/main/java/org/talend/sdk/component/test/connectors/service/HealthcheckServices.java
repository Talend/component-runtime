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
package org.talend.sdk.component.test.connectors.service;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus.Status;
import org.talend.sdk.component.test.connectors.config.TheDatastore;

@Service
public class HealthcheckServices {

    /**
     * In this service sample class we will implement existing particular actions to check their API usages.
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - Healthcheck https://talend.github.io/component-runtime/main/latest/services-actions.html#_healthcheck
     *
     */

    public final static String HEALTH_CHECK_OK = "action_HEALTH_CHECK_OK";

    public final static String HEALTH_CHECK_KO = "action_HEALTH_CHECK_KO";

    /**
     * Healthcheck action OK
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_healthcheck
     * Type: healthcheck
     * API: @org.talend.sdk.component.api.service.healthcheck.HealthCheck
     *
     * Returned type: org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus
     */
    @HealthCheck(HEALTH_CHECK_OK)
    public HealthCheckStatus discoverHealthcheckOk(@Option final TheDatastore unused) {
        return new HealthCheckStatus(Status.OK, "HealthCheck Success");
    }

    /**
     * Healthcheck action KO
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/services-actions.html#_healthcheck
     * Type: healthcheck
     * API: @org.talend.sdk.component.api.service.healthcheck.HealthCheck
     *
     * Returned type: org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus
     */
    @HealthCheck(HEALTH_CHECK_KO)
    public HealthCheckStatus discoverHealthcheckKo(@Option final TheDatastore unused) {
        return new HealthCheckStatus(Status.KO, "HealthCheck Failed");
    }

}
