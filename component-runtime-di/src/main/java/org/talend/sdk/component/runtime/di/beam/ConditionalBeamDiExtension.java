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

import static java.util.Optional.ofNullable;

import java.util.Map;

import org.talend.sdk.component.spi.component.ComponentExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionalBeamDiExtension implements ComponentExtension {

    private final ComponentExtension delegate = findDelegate();

    private ComponentExtension findDelegate() {
        try {
            ofNullable(ConditionalBeamDiExtension.class.getClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader)
                    .loadClass("org.apache.beam.sdk.transforms.PTransform");
            return new BeamDiExtension();
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            log.warn(e.getMessage(), e);
            return new ComponentExtension() {

                @Override
                public void onComponent(final ComponentContext context) {
                    // no-op
                }

                @Override
                public boolean supports(final Class<?> componentType) {
                    return false;
                }

                @Override
                public <T> T convert(final ComponentInstance instance, final Class<T> component) {
                    throw new UnsupportedOperationException();
                }
            };
        }
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
    public <T> T convert(final ComponentInstance instance, final Class<T> component) {
        return delegate.convert(instance, component);
    }

    @Override
    public Map<Class<?>, Object> getExtensionServices(final String plugin) {
        return delegate.getExtensionServices(plugin);
    }

    @Override
    public int priority() {
        return delegate.priority();
    }

    @Override
    public <T> T unwrap(final Class<T> type, final Object... args) {
        return delegate.unwrap(type, args);
    }
}
