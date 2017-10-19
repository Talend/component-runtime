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
package org.talend.components.runtime.beam.spi;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.talend.components.runtime.beam.impl.BeamMapperImpl;
import org.talend.components.runtime.beam.impl.BeamProcessorChainImpl;
import org.talend.components.runtime.input.Mapper;
import org.talend.components.runtime.output.Processor;
import org.talend.components.spi.component.ComponentExtension;

// TODO: enrich the validation set to check the from/to generics in the pTransforms
public class BeamComponentExtension implements ComponentExtension {

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
    public <T> T convert(final ComponentInstance instance, final Class<T> component) {
        if (Mapper.class == component) {
            return (T) new BeamMapperImpl((PTransform<PBegin, ?>) instance.instance(), instance.plugin(), instance.family(),
                    instance.name());
        }
        if (Processor.class == component) {
            return (T) new BeamProcessorChainImpl((PTransform<PCollection<?>, PDone>) instance.instance(), null,
                    instance.plugin(), instance.family(), instance.name());
        }
        throw new IllegalArgumentException("unsupported " + component + " by " + getClass());
    }
}
