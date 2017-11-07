/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.server.front.monitoring;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import brave.http.HttpTracing;
import brave.jaxrs2.TracingFeature;

@Dependent
@Provider
public class BraveFeature implements Feature {

    @Inject
    private HttpTracing tracing;

    @Override
    public boolean configure(final FeatureContext context) {
        TracingFeature.create(tracing).configure(context);
        return true;
    }
}
