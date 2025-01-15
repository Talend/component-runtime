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
package org.talend.sdk.component.server.front.filter.message;

import javax.enterprise.context.Dependent;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
@Dependent
public class MessageResponseFilter implements DynamicFeature {

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        if (resourceInfo.getResourceMethod().isAnnotationPresent(Deprecated.class)
                || resourceInfo.getResourceClass().isAnnotationPresent(Deprecated.class)) {
            context
                    .register((ContainerResponseFilter) (requestContext, responseContext) -> responseContext
                            .getHeaders()
                            .putSingle("X-Talend-Warning",
                                    "This endpoint is deprecated and will be removed without notice soon."));
        }
    }
}
