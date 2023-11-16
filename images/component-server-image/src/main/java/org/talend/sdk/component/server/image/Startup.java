/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.image;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class Startup {

    public void onStart(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        log.info("Starting...");
        if (log.isDebugEnabled()) {
            log.debug("System properties:");
            System
                    .getProperties()
                    .entrySet()
                    .stream()
                    .forEach(e -> log.debug(String.format("===> %s=%s", e, e.getValue())));
            log.debug("Environment:");
            System
                    .getenv()
                    .entrySet()
                    .stream()
                    .forEach(e -> log.debug(String.format("===> %s=%s", e.getKey(), e.getValue())));
        }
    }
}
