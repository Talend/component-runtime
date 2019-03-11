/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;

import org.apache.meecrowave.Meecrowave;

@ApplicationScoped
public class InitStitch {

    public void onStart(@Observes @Initialized(ApplicationScoped.class) @Priority(2000) final ServletContext context,
            final Meecrowave.Builder config) {
        System
                .setProperty("talend.server.extension.stitch.client.base",
                        "http://localhost:" + config.getHttpPort() + "/api/v1/");
        System.setProperty("talend.server.extension.stitch.token", "test-token");
        System.setProperty("talend.server.extension.stitch.client.retries", "5");
        System.setProperty("talend.component.server.extension.stitch.versionMarker", Long.toString(System.nanoTime()));
    }
}
