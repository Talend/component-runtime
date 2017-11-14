/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.design.extension;

import java.util.stream.Stream;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.spi.ContainerListenerExtension;

/**
 * Service provider for {@link ContainerListenerExtension} service
 */
public class DesignContainerListener implements ContainerListenerExtension {

    /**
     * Enriches {@link Container} with {@link DesignModelRegistry}
     * It depends on Updater listener which adds {@link ContainerComponentRegistry} class to {@link Container}
     */
    @Override
    public void onCreate(Container container) {
        ContainerComponentRegistry componentRegistry = container.get(ContainerComponentRegistry.class);
        if (componentRegistry == null) {
            throw new IllegalArgumentException("container doesn't contain ContainerComponentRegistry");
        }

        componentRegistry.getComponents().values().stream()
                .flatMap(family -> Stream.concat( //
                        family.getPartitionMappers().values().stream(), //
                        family.getProcessors().values().stream())) //
                .forEach(meta -> meta.set(DesignModel.class,
                        new DesignModel( //
                                meta.getId(), //
                                meta.getInputFlows(), //
                                meta.getOutputFlows()))); //
    }

    /**
     * It relies on Updater listener onClose() method which removes all ComponentFamilyMeta from Container
     * Thus, this listener has nothing to do on close
     */
    @Override
    public void onClose(Container container) {
        // no-op
    }

}
