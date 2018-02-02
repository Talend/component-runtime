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
package org.talend.sdk.component.spi.component;

import static java.util.Collections.emptyMap;

import java.util.Map;

/**
 * Provide a way to interact with component scanning and metadata extraction.
 *
 * Note: it requires to use
 * org.talend.sdk.component.runtime.manager.ComponentManager to be activated.
 */
public interface ComponentExtension {

    /**
     * @param context
     * the component context allowing to interact with the container.
     */
    void onComponent(ComponentContext context);

    /**
     * @param componentType
     * the expected framework component type (can be Mapper or
     * Processor).
     * @return true if convert can be used for that kind of component, false
     * otherwise.
     */
    boolean supports(Class<?> componentType);

    /**
     * Note: you can assume supports() was called before going into this method.
     *
     * @param instance
     * the instantiated component (native instance).
     * @param component
     * the expected framework component type (can be Mapper or
     * Processor).
     * @param <T>
     * the generic matching component parameter.
     * @return an instance of component.
     */
    <T> T convert(ComponentInstance instance, Class<T> component);

    /**
     * @return the services specific to the extension;
     */
    default Map<Class<?>, Object> getExtensionServices() {
        return emptyMap();
    }

    /**
     * This is the handle giving the extension information about the component being
     * processed and allowing to interact with the container lifecycle.
     */
    interface ComponentInstance {

        /**
         * @return the component native instance.
         */
        Object instance();

        /**
         * @return the plugin identifier of the component.
         */
        String plugin();

        /**
         * @return the family identifier of the component.
         */
        String family();

        /**
         * @return the name identifier of the component.
         */
        String name();
    }

    /**
     * This is the handle giving the extension information about the component being
     * processed and allowing to interact with the container lifecycle.
     */
    interface ComponentContext {

        /**
         * @return the class representing the component.
         */
        Class<?> getType();

        /**
         * will prevent the component to be usable with findMapper()/findProcessor() but
         * will also deactivate the associated validation so you can
         * use @PartitionMapper and @Processor for another runtime than the framework
         * default one.
         */
        void skipValidation();
    }
}
