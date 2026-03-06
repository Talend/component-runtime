/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.stream.Stream;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.flows.FlowsFactory;
import org.talend.sdk.component.design.extension.repository.RepositoryModelBuilder;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.extension.ComponentContexts;
import org.talend.sdk.component.runtime.manager.reflect.MigrationHandlerFactory;
import org.talend.sdk.component.runtime.manager.spi.ContainerListenerExtension;
import org.talend.sdk.component.spi.component.ComponentExtension;

/**
 * Service provider for {@link ContainerListenerExtension} service
 */
public class DesignContainerListener implements ContainerListenerExtension {

    private final RepositoryModelBuilder repositoryModelBuilder = new RepositoryModelBuilder();

    private MigrationHandlerFactory migrationHandlerFactory;

    @Override
    public void setComponentManager(final ComponentManager manager) {
        migrationHandlerFactory = manager.getMigrationHandlerFactory();
    }

    /**
     * Enriches {@link Container} with {@link DesignModel} and
     * {@link RepositoryModel} It depends on Updater listener which adds
     * {@link ContainerComponentRegistry} class to {@link Container}
     */
    @Override
    public void onCreate(final Container container) {
        final ContainerComponentRegistry componentRegistry = container.get(ContainerComponentRegistry.class);

        if (componentRegistry == null) {
            throw new IllegalArgumentException("container doesn't contain ContainerComponentRegistry");
        }

        final Collection<ComponentFamilyMeta> componentFamilyMetas = componentRegistry.getComponents().values();

        // Create Design Model
        componentFamilyMetas
                .stream()
                .flatMap(family -> Stream
                        .of(family.getPartitionMappers().values().stream(), family.getProcessors().values().stream(),
                                family.getDriverRunners().values().stream())
                        .flatMap(t -> t))
                .forEach(meta -> {
                    final ComponentExtension.ComponentContext context =
                            container.get(ComponentContexts.class).getContexts().get(meta.getType());
                    final ComponentExtension owningExtension = context.owningExtension();
                    meta
                            .set(DesignModel.class, ofNullable(owningExtension)
                                    .map(e -> e.unwrap(FlowsFactory.class, meta))
                                    .map(e -> new DesignModel(meta.getId(), e.getInputFlows(), e.getOutputFlows()))
                                    .orElseGet(() -> {
                                        final FlowsFactory factory = FlowsFactory.get(meta);
                                        return new DesignModel(meta.getId(), factory.getInputFlows(),
                                                factory.getOutputFlows());
                                    }));
                });

        // Create Repository Model
        container
                .set(RepositoryModel.class,
                        repositoryModelBuilder
                                .create(container.get(ComponentManager.AllServices.class), componentFamilyMetas,
                                        migrationHandlerFactory));
    }

    /**
     * It relies on Updater listener onClose() method which removes all
     * ComponentFamilyMeta from Container Thus, this listener has nothing to do on
     * close
     */
    @Override
    public void onClose(final Container container) {
        // no-op
    }

}