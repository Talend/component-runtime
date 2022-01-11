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
package org.talend.sdk.component.runtime.manager.finder;

import java.util.Optional;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.serialization.LightContainer;
import org.talend.sdk.component.runtime.serialization.TCCLContainerFinder;

import lombok.extern.slf4j.Slf4j;

// NOTE: ensure it is aligned with the components, this is hardcoded cause must be standard!

/**
 * a {@link org.talend.sdk.component.runtime.serialization.ContainerFinder}
 * which will starts and manage a single {@link ComponentManager} for the JVM
 * (root classloader actually) life.
 */
@Slf4j
public class StandaloneContainerFinder extends TCCLContainerFinder {

    // IMPORTANT: don't abuse of lambdas here, it is on the runtime codepath
    @Override
    public LightContainer find(final String plugin) {
        final ComponentManager manager = ComponentManager.instance();
        Optional<Container> optionalContainer = manager.findPlugin(plugin);
        if (!optionalContainer.isPresent()) {
            log.info("Didn't find plugin " + plugin + ", had: " + manager.availablePlugins());

            // we assume we use a fatjar created with nested-maven-repository extensions
            // (default nested loading)
            // so we have the plugin in TALEND-INF/plugins.properties and the jar located as
            // nested in current jar.
            try {
                optionalContainer = manager.findPlugin(manager.addPlugin(plugin));
            } catch (final IllegalArgumentException iae) { // concurrent request?
                optionalContainer = manager.findPlugin(plugin);
            }
        }
        if (optionalContainer.isPresent()) {
            final LightContainer lightContainer = optionalContainer.get().get(LightContainer.class);
            if (lightContainer != null) {
                return lightContainer;
            }
        }
        return super.find(plugin); // TCCL
    }
}
