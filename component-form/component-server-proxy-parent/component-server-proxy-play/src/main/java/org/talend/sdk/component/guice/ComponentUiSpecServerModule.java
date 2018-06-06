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
package org.talend.sdk.component.guice;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

import org.talend.sdk.component.proxy.api.service.Services;

public class ComponentUiSpecServerModule extends AbstractModule {

    @Override
    protected void configure() {
        Stream.of(Services.class.getMethods()).filter(m -> m.isAnnotationPresent(Services.Binding.class)).forEach(m -> {
            final Class type = m.getReturnType();
            final Provider<?> binding = () -> {
                try {
                    return m.invoke(null);
                } catch (final InvocationTargetException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            };
            doBind(type, binding);
        });
    }

    private <T> void doBind(final Class<T> type, final Provider<T> instance) {
        bind(type).toProvider(instance);
    }
}
