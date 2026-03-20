/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service.dependency;

import java.util.function.Predicate;

public interface ClassLoaderDefinition {

    /**
     *
     * @return the parent classloader to use for the classloader to create.
     */
    ClassLoader getParent();

    /**
     *
     * @return a filter to apply on the classes to load from the classloader to create. If the filter returns false for
     * a class, the classloader to create will try to load it from its parent.
     */
    Predicate<String> getClassesFilter();

    /**
     *
     * @return a filter to apply on the classes to load from the parent classloader. If the filter returns false for a
     * class, the classloader to create will try to load it by itself instead of delegating to its parent.
     */
    Predicate<String> getParentClassesFilter();

    /**
     *
     * @return a filter to apply on the resources to load from the parent classloader. If the filter returns false for a
     * resource, the classloader to create will try to load it by itself instead of delegating to its parent.
     */
    Predicate<String> getParentResourcesFilter();

    /**
     *
     * @return true if the classloader to create should support resource dependencies (i.e. the dependencies.txt can
     * also list resources to load and not only classes). Note that if this is true, the classloader to create will try
     * to load resources by itself instead of delegating to its parent.
     *
     */
    boolean isSupportsResourceDependencies();

    /**
     *
     * @return the resource path to the plugins mapping file ("TALEND-INF/plugins.properties").
     */
    String getNestedPluginMappingResource();
}
