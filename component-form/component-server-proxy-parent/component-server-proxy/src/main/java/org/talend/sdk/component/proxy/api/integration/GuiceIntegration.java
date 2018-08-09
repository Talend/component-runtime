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
package org.talend.sdk.component.proxy.api.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;

@Vetoed
public class GuiceIntegration implements Integration {

    private final Function<Class<?>, Object> impl;

    public GuiceIntegration() {
        try {
            final Class<?> injector = GuiceIntegration.class.getClassLoader().loadClass("com.google.inject.Injector");
            final Method getInstance = injector.getMethod("getInstance", Class.class);
            impl = new Function<Class<?>, Object>() {

                private final AtomicReference<Object> instance = new AtomicReference<>();

                @Override
                public Object apply(final Class<?> type) {
                    Object o = instance.get();
                    if (o == null) {
                        o = CDI.current().select(injector).get();
                        instance.compareAndSet(null, o);
                    }
                    try {
                        return getInstance.invoke(o, type);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    } catch (final InvocationTargetException e) {
                        throw new IllegalStateException(e.getTargetException());
                    }
                }
            };
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T lookup(final Class<T> type) {
        return type.cast(impl.apply(type));
    }
}
