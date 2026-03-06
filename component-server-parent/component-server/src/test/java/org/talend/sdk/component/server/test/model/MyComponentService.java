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
package org.talend.sdk.component.server.test.model;

import java.util.Date;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;

@Service
public class MyComponentService {

    @HealthCheck(family = "chain")
    public HealthCheckStatus doCheck(final ListInput.MyDataSet dataSet) {
        if (dataSet == null || dataSet.getUrls() == null) {
            throw new IllegalStateException("simulating an unexpected error");
        }
        return new HealthCheckStatus(HealthCheckStatus.Status.OK, "All good");
    }

    @HealthCheck(family = "chain", value = "langtest")
    public HealthCheckStatus doCheckWithLang(@Option("$lang") final String lang) {
        return new HealthCheckStatus(HealthCheckStatus.Status.OK, lang);
    }

    public void logTime() { // usable by components
        System.out.println(new Date());
    }
}
