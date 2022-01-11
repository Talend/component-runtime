/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;

@MonoMeecrowaveConfig
class ActionDisplayNameTest {

    @Inject
    private ComponentManagerService componentManagerService;

    @Test
    void actionDisplayName() {
        final Container container = componentManagerService
                .manager()
                .findPlugin("jdbc-component")
                .orElseThrow(() -> new IllegalStateException("No jdbc plugin"));
        final String displayName = container
                .get(ContainerComponentRegistry.class)
                .getComponents()
                .values()
                .iterator()
                .next()
                .findBundle(container.getLoader(), Locale.ROOT)
                .actionDisplayName("user", "custom")
                .orElseThrow(() -> new IllegalStateException("No custom i18n"));
        assertEquals("My Custom Action", displayName);
    }
}
