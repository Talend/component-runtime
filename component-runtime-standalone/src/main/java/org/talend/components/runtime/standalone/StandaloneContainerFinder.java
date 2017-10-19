// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.standalone;

import static java.util.Optional.ofNullable;

import lombok.extern.slf4j.Slf4j;

import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.serialization.LightContainer;
import org.talend.components.runtime.serialization.TCCLContainerFinder;

// NOTE: ensure it is aligned with the components, this is hardcoded cause must be standard!

/**
 * a {@link org.talend.components.runtime.serialization.ContainerFinder} which will starts and manage a
 * single {@link ComponentManager} for the JVM (root classloader actually) life.
 */
@Slf4j
public class StandaloneContainerFinder extends TCCLContainerFinder {

    @Override
    public LightContainer find(final String plugin) {
        final ComponentManager manager = ComponentManager.instance();
        return ofNullable(manager.findPlugin(plugin).orElseGet(() -> {
            log.info("Didn't find plugin " + plugin + ", had: " + manager.availablePlugins());

            // we assume we use a fatjar created with nested-maven-repository extensions (default nested loading)
            // so we have the plugin in TALEND-INF/plugins.properties and the jar located as nested in current jar.
            try {
                return manager.findPlugin(manager.addPlugin(plugin)).orElse(null);
            } catch (final IllegalArgumentException iae) { // concurrent request?
                return manager.findPlugin(plugin).orElse(null);
            }
        })).map(container -> container.get(LightContainer.class)).orElseGet(() -> super.find(plugin));
    }
}
