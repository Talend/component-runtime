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
package org.talend.sdk.component.runtime.di.beam;

import org.talend.sdk.component.runtime.beam.spi.BeamComponentExtension;
import org.talend.sdk.component.runtime.di.beam.components.QueueMapper;
import org.talend.sdk.component.runtime.di.beam.components.QueueOutput;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Processor;

public class BeamDiExtension extends BeamComponentExtension {

    @Override
    public int priority() {
        return super.priority() - 1;
    }

    @Override
    public <T> T convert(final ComponentInstance instance, final Class<T> component) {
        if (Mapper.class == component) {
            final org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PBegin, org.apache.beam.sdk.values.PCollection<?>> begin =
                    (org.apache.beam.sdk.transforms.PTransform<org.apache.beam.sdk.values.PBegin, org.apache.beam.sdk.values.PCollection<?>>) instance
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
}
