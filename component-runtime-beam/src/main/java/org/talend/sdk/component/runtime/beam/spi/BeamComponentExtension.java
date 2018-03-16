/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.spi;

import java.util.HashMap;
import java.util.Map;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.talend.sdk.component.runtime.beam.design.BeamFlowFactory;
import org.talend.sdk.component.runtime.beam.factory.service.AutoValueFluentApiFactory;
import org.talend.sdk.component.runtime.beam.factory.service.PluginCoderFactory;
import org.talend.sdk.component.runtime.beam.impl.BeamMapperImpl;
import org.talend.sdk.component.runtime.beam.impl.BeamProcessorChainImpl;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.spi.component.ComponentExtension;

// TODO: enrich the validation set to check the from/to generics in the pTransforms
public class BeamComponentExtension implements ComponentExtension {

    @Override
    public <T> T unwrap(final Class<T> type, final Object... args) {
        if ("org.talend.sdk.component.design.extension.flows.FlowsFactory".equals(type.getName()) && args != null
                && args.length == 1 && ComponentFamilyMeta.BaseMeta.class.isInstance(args[0])) {
            if (ComponentFamilyMeta.ProcessorMeta.class.isInstance(args[0])) {
                return type.cast(BeamFlowFactory.OUTPUT);
            }
            return type.cast(BeamFlowFactory.INPUT);
        }
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }

    @Override
    public void onComponent(final ComponentContext context) {
        if (PTransform.class.isAssignableFrom(context.getType())) {
            context.skipValidation();
        }
    }

    @Override
    public boolean supports(final Class<?> componentType) {
        return componentType == Mapper.class || componentType == Processor.class;
    }

    @Override
    public Map<Class<?>, Object> getExtensionServices(final String plugin) {
        return new HashMap<Class<?>, Object>() {

            {
                put(AutoValueFluentApiFactory.class, new AutoValueFluentApiFactory());
                put(PluginCoderFactory.class, new PluginCoderFactory(plugin));
            }
        };
    }

    @Override
    public <T> T convert(final ComponentInstance instance, final Class<T> component) {
        if (Mapper.class == component) {
            return (T) new BeamMapperImpl((PTransform<PBegin, ?>) instance.instance(), instance.plugin(),
                    instance.family(), instance.name());
        }
        if (Processor.class == component) {
            return (T) new BeamProcessorChainImpl((PTransform<PCollection<?>, PDone>) instance.instance(), null,
                    instance.plugin(), instance.family(), instance.name());
        }
        throw new IllegalArgumentException("unsupported " + component + " by " + getClass());
    }
}
