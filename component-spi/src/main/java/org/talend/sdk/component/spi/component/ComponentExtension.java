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
package org.talend.sdk.component.spi.component;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.lang.instrument.ClassFileTransformer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Provide a way to interact with component scanning and metadata extraction.
 *
 * Note: it requires to use
 * org.talend.sdk.component.runtime.manager.ComponentManager to be activated.
 */
public interface ComponentExtension {

    /**
     * @return true if the extension can be used in current environment.
     */
    default boolean isActive() {
        return true;
    }

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
     * @param plugin the plugin to enrich with services.
     * @return the services specific to the extension.
     */
    default Map<Class<?>, Object> getExtensionServices(final String plugin) {
        return emptyMap();
    }

    /**
     * The priority of the extension.
     * Extensions are sorted by priority and the first one matching (supports) wins.
     *
     * @return the priority for this extension, smaller is the highest priority.
     */
    default int priority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Unwrap the current instance to another type. Useful to access advanced features of some extensions.
     *
     * @param type the expected type.
     * @param args optional parameters for the unwrapping.
     * @param <T> the type to cast the extension to.
     * @return the unwrapped instance or null if not supported.
     */
    default <T> T unwrap(final Class<T> type, final Object... args) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        return null;
    }

    /**
     * @return a list of transformer to set on the component classloader.
     */
    default Collection<ClassFileTransformer> getTransformers() {
        return emptyList();
    }

    /**
     * @return a Stream of dependencies coordinates
     */
    default Collection<String> getAdditionalDependencies() {
        return Collections.emptyList();
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

        /**
         * Useful for extensions needing to access metadata from another programming model.
         * Exposing the extension allows to unwrap it to access it.
         *
         * @return null if no extension owns the component, the extension instance otherwise.
         */
        ComponentExtension owningExtension();
    }
}
