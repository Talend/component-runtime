/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.design.extension.flows;

import java.util.Collection;

import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.BaseMeta;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.DriverRunnerMeta;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.PartitionMapperMeta;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.ProcessorMeta;

/**
 * Strategy creates component flows according component type (either
 * {@link ProcessorMeta} or {@link PartitionMapperMeta})
 */
public interface FlowsFactory {

    /**
     * Creates appropriate factory according {@link BaseMeta} type (either
     * {@link ProcessorMeta} or {@link PartitionMapperMeta})
     * 
     * @param meta the meta instance to use as reference to find the right factory.
     * @return the factory to use to create a flow for this meta.
     */
    static FlowsFactory get(final BaseMeta<? extends Lifecycle> meta) {
        if (meta == null) {
            throw new IllegalArgumentException("meta should not be null");
        }
        if (PartitionMapperMeta.class.isInstance(meta)) {
            return new PartitionMapperFlowsFactory();
        }
        if (ProcessorMeta.class.isInstance(meta)) {
            return new ProcessorFlowsFactory(meta.getType());
        }
        if (DriverRunnerMeta.class.isInstance(meta)) {
            return new DriverRunnerFlowsFactory();
        }
        throw new IllegalArgumentException("unknown meta type " + meta.getClass().getName());
    }

    /**
     * Returns a {@link Collection} of input flows names of a Component
     * 
     * @return input flows names collection
     */
    Collection<String> getInputFlows();

    /**
     * Returns a {@link Collection} of output flows names of a Component
     * 
     * @return output flows names collection
     */
    Collection<String> getOutputFlows();
}
