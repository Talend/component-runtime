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
package org.talend.sdk.component.api.service.factory;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Allows to create an instance from a classname and a set of properties.
 * The implementation handles the coercing if needed.
 *
 * Note that it doesn't reuse the configuration format (for now) since it is intended
 * to be used to convert some configuration (UI) properties to an instance.
 */
public interface ObjectFactory {

    /**
     * Creates an instance builder.
     * 
     * @param className the class of the instance to create.
     * @return a builder you can configure before instantiating the instance.
     */
    ObjectFactoryInstance createInstance(String className);

    interface ObjectFactoryInstance {

        /**
         * Set that the properties can be injected into fields directly, default uses fields
         * but not private ones.
         * 
         * @return this.
         */
        ObjectFactoryInstance withFieldInjection();

        /**
         * Set that the properties can NOT be injected into fields directly.
         *
         * @return this.
         */
        ObjectFactoryInstance withoutFieldInjection();

        /**
         * If called the unsupported properties will be ignored otherwise {@link ObjectFactoryInstance#create(Class)}
         * will fail with an exception.
         *
         * @return this.
         */
        ObjectFactoryInstance ignoreUnknownProperties();

        /**
         * @param properties the properties to use to configure the instance.
         * @return this.
         */
        ObjectFactoryInstance withProperties(Map<String, ?> properties);

        /**
         * Shortcut for {@link ObjectFactoryInstance#withProperties(Map)}.
         * Note that if there are conflicting keys the last one is used.
         *
         * @param stream a stream of properties.
         * @param keyExtractor how to extract the key from the incoming stream.
         * @param valueExtractor how to extract the value from the incoming stream.
         * @param <T> the type of the created object.
         * @return this.
         */
        default <T> ObjectFactoryInstance withProperties(final Stream<T> stream, final Function<T, String> keyExtractor,
                final Function<T, ?> valueExtractor) {
            return withProperties(ofNullable(stream)
                    .orElseGet(Stream::empty)
                    .collect(toMap(keyExtractor, valueExtractor, (a, b) -> a)));
        }

        /**
         * @param parentType the type to cast the instance to.
         * @param <T> the expected type of the returned instance.
         * @return the created instance.
         */
        <T> T create(final Class<T> parentType);
    }
}
