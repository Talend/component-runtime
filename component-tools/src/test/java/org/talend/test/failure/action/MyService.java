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
package org.talend.test.failure.action;

import java.io.Serializable;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;

@Service
public class MyService implements Serializable {

    @HealthCheck
    public String /* should be health check status */ test(final MyDataStore datastore) {
        return null;
    }
}
