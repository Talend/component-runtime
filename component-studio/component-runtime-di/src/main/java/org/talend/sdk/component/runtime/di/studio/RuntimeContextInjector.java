/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.studio;

import org.talend.sdk.component.api.context.RuntimeContext;
import org.talend.sdk.component.api.context.RuntimeContextHolder;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RuntimeContextInjector {

    /**
     * Inject runtime context object to runtime input/processor/standalone runner object
     *
     * @param lifecycle input/processor/standalone runner
     * @param runtimeContext the runtime context
     * @see Lifecycle
     */
    public static void injectLifecycle(final Lifecycle lifecycle, final RuntimeContextHolder runtimeContext) {
        if (lifecycle instanceof Delegated) {
            final Object delegate = ((Delegated) lifecycle).getDelegate();

            Class<?> currentClass = delegate.getClass();
            while (currentClass != null && currentClass != Object.class) {
                ReflectionUtils.findFields(currentClass, RuntimeContext.class).forEach(f -> {
                    try {
                        f.set(delegate, runtimeContext);
                        return;
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                });

                currentClass = currentClass.getSuperclass();
            }
        } else {
            throw new IllegalArgumentException("Not supported implementation of lifecycle : " + lifecycle);
        }
    }

    /**
     * Inject runtime context object to runtime connection/close service/action object
     *
     * @param manager component manager
     * @param plugin the plugin name
     * @param runtimeContext the runtime context
     * @see ComponentManager
     */
    public static void injectService(final ComponentManager manager, final String plugin,
            final RuntimeContextHolder runtimeContext) {
        manager.findPlugin(plugin)
                .orElseThrow(() -> new IllegalStateException("Can't find the plugin : " + plugin))
                .get(org.talend.sdk.component.runtime.manager.ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .forEach(service -> {
                    ReflectionUtils.findFields(service.getInstance(), RuntimeContext.class).forEach(f -> {
                        try {
                            f.set(service.getInstance(), runtimeContext);
                            return;
                        } catch (final IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                });

    }

}
