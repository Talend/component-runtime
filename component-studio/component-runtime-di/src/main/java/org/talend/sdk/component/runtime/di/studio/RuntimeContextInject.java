/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import java.util.Map;

import org.talend.sdk.component.api.context.RuntimeContext;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RuntimeContextInject {

    /**
     * Inject runtime context object to runtime input/processor/standalone runner object
     */
    public static void injectRuntimeContext(final Lifecycle lifecycle, final Map<String, Object> runtimeContext) {
        if (lifecycle instanceof Delegated) {
            final Object delegate = ((Delegated) lifecycle).getDelegate();

            Class<?> currentClass = delegate.getClass();
            while (currentClass != null && currentClass != Object.class) {
                Utils.findFields(delegate, RuntimeContext.class).forEach(f -> {
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
            log.warn("Not supported implementation of lifecycle");
        }
    }

    /**
     * Inject runtime context object to runtime connection/close service/action object,
     * maybe also commit/rollback if they are auto generated too, TODO consider
     * service is flat, no extends as common, so not do getSuperclass
     */
    public static void injectRuntimeContextForService(final ComponentManager manager, final String plugin,
            final Map<String, Object> runtimeContext) {
        manager.findPlugin(plugin)
                .get()
                .get(org.talend.sdk.component.runtime.manager.ContainerComponentRegistry.class)
                .getServices()
                .stream()
                .forEach(service -> {
                    Utils.findFields(service.getInstance(), RuntimeContext.class).forEach(f -> {
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
