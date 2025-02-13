/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.di.studio;

import org.talend.sdk.component.runtime.manager.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class TacokitAvailableOutputFlows {

    private ComponentManager componentManager;

    private Map<String, String> configuration;

    private String plugin;

    private String family;

    private String componentName;

    private String action;

    private final Integer version;

    public static final String NO_COMPONENT = "No component ";

    public static final String TCOMP_CONFIGURATIONTYPE_TYPE = "tcomp::configurationtype::type";
    private static final String AVALIALBE_OUTPUT_FLOWS = "AvailableOutputFlows";

    public TacokitAvailableOutputFlows(final Map<String, String> configuration, final String plugin,
                                       final String family, final String componentName, final String action, final String version) {
        this.componentManager = ComponentManager.instance();
        this.componentManager.autoDiscoverPlugins(false, true);
        this.configuration = configuration;
        this.plugin = plugin;
        this.family = family;
        this.componentName = componentName;
        this.action = action;
        this.version = Optional.ofNullable(version).map(Integer::parseInt).orElse(null);
    }

    public Collection<String> getAvailableFlows() {
        final Collection<ServiceMeta> services = getPluginServices();
        ServiceMeta.ActionMeta actionRef = services
                .stream()
                .flatMap(s -> s.getActions().stream())
                .filter(a -> a.getFamily().equals(family) &&
                        a.getType().equals(AVALIALBE_OUTPUT_FLOWS) &&
                        componentName.equals(a.getAction()))
                .findFirst()
                .orElse(null);
        // did not find action named like componentName, trying to find one matching action...
        if (actionRef == null) {
            actionRef = services
                    .stream()
                    .flatMap(s -> s.getActions().stream())
                    .filter(a -> a.getFamily().equals(family) && a.getType().equals(AVALIALBE_OUTPUT_FLOWS))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No action " + family + "#" + AVALIALBE_OUTPUT_FLOWS));
        }
        final Object schemaResult =
                actionRef.getInvoker().apply(buildActionConfig(actionRef, configuration));
        return null;
    }

    private Map<String, String> buildActionConfig(final ServiceMeta.ActionMeta action,
                                                  final Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return configuration; // no-mapping
        }

        final String prefix = action
                .getParameters()
                .get()
                .stream()
//                .filter(param -> param.getMetadata().containsKey(TCOMP_CONFIGURATIONTYPE_TYPE)
//                        && DATASET.equals(param.getMetadata().get(TCOMP_CONFIGURATIONTYPE_TYPE)))
                .findFirst()
                .map(ParameterMeta::getPath)
                .orElse(null);

        if (prefix == null) { // no mapping to do
            return configuration;
        }

        return configuration
                .entrySet()
                .stream()
                //.filter(e -> isChildParameter(e.getKey(), dataSetPath))
                .collect(toMap(e -> prefix + e.getKey(), Map.Entry::getValue));
    }

    private ComponentFamilyMeta.BaseMeta<?> findComponent(final ComponentFamilyMeta familyMeta) {
        return Stream
                .concat(familyMeta.getPartitionMappers().entrySet().stream(),
                        familyMeta.getProcessors().entrySet().stream())
                .filter(e -> e.getKey().equals(componentName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(NO_COMPONENT + componentName));
    }

    private ComponentFamilyMeta findFamily() {
        return componentManager
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException("No component family " + plugin))
                .get(ContainerComponentRegistry.class)
                .getComponents()
                .get(family);
    }

    private Collection<ServiceMeta> getPluginServices() {
        return componentManager
                .findPlugin(plugin)
                .orElseThrow(() -> new IllegalArgumentException(NO_COMPONENT + plugin))
                .get(ContainerComponentRegistry.class)
                .getServices();
    }
}
 