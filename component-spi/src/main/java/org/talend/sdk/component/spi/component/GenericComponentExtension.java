/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.spi.component;

import java.util.Map;

/**
 * Generic way to create components.
 * This is to use when all components of the same family can be instantiated the exact same way.
 * However please note that you should only use that for virtual components and fully understand what it means
 * because the code path for this kind of extension has no link with the standard one.
 */
// @Internal
public interface GenericComponentExtension {

    boolean canHandle(Class<?> expectedType, String plugin, String name);

    <T> T createInstance(Class<T> type, String plugin, String name, int version, Map<String, String> configuration,
            Map<Class<?>, Object> services);
}
