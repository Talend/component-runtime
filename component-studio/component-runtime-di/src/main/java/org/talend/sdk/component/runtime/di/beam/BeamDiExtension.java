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
package org.talend.sdk.component.runtime.di.beam;

import static java.util.Collections.emptyList;

import java.lang.instrument.ClassFileTransformer;
import java.util.Collection;
import java.util.Map;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.di.beam.components.QueueMapper;
import org.talend.sdk.component.runtime.di.beam.components.QueueOutput;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.spi.component.ComponentExtension;

public class BeamDiExtension implements ComponentExtension {

    private ComponentExtension delegate;

    @Override
    public int priority() {
        return delegate.priority() - 1;
    }

    @Override
    public boolean isActive() {
        try {
            delegate = new org.talend.sdk.component.runtime.beam.spi.BeamComponentExtension() {

                @Override
                public <T> T convert(final ComponentInstance instance, final Class<T> component) {
                    return doConvert(instance, component);
                }
            };
        } catch (final NoClassDefFoundError | RuntimeException e) {
            return false;
        }
        return delegate.isActive();
    }

    private <T> T doConvert(final ComponentInstance instance, final Class<T> component) {
        if (Mapper.class == component) {
            final org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PBegin, org.apache.beam.sdk.values.PCollection<Record>> begin =
                    (org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PBegin, org.apache.beam.sdk.values.PCollection<Record>>) instance
                            .instance();
            return component.cast(new QueueMapper(instance.plugin(), instance.family(), instance.name(), begin));
        }
        if (Processor.class == component) {
            final org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PCollection<?>, org.apache.beam.sdk.values.PDone> transform =
                    (org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PCollection<?>, org.apache.beam.sdk.values.PDone>) instance
                            .instance();
            return component.cast(new QueueOutput(instance.plugin(), instance.family(), instance.name(), transform));
        }
        throw new IllegalArgumentException("unsupported " + component + " by " + getClass());
    }

    @Override
    public <T> T unwrap(final Class<T> type, final Object... args) {
        return delegate.unwrap(type, args);
    }

    @Override
    public Collection<ClassFileTransformer> getTransformers() {
        return emptyList(); // parent ones are added, if we add it back we can get corrupted bytecode
    }

    @Override
    public void onComponent(final ComponentContext context) {
        delegate.onComponent(context);
    }

    @Override
    public boolean supports(final Class<?> componentType) {
        return delegate.supports(componentType);
    }

    @Override
    public Map<Class<?>, Object> getExtensionServices(final String plugin) {
        return delegate.getExtensionServices(plugin);
    }

    @Override
    public <T> T convert(final ComponentInstance instance, final Class<T> component) {
        return delegate.convert(instance, component);
    }

    @Override
    public Collection<String> getAdditionalDependencies() {
        return delegate.getAdditionalDependencies();
    }
}
