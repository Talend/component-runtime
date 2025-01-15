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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toSet;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Option;
import org.talend.sdk.component.api.service.factory.ObjectFactory;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ObjectFactoryImpl implements ObjectFactory, Serializable {

    private final String plugin;

    private final PropertyEditorRegistry registry;

    @Override
    public ObjectFactoryInstance createInstance(final String type) {
        final ObjectRecipe recipe = new ObjectRecipe(type);
        recipe.setRegistry(registry);
        return new ObjectFactoryInstanceImpl(recipe);
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, ObjectFactory.class.getName());
    }

    @AllArgsConstructor
    private static class ObjectFactoryInstanceImpl implements ObjectFactoryInstance {

        private final ObjectRecipe recipe;

        @Override
        public ObjectFactoryInstance withFieldInjection() {
            recipe.allow(Option.FIELD_INJECTION);
            recipe.allow(Option.PRIVATE_PROPERTIES);
            return this;
        }

        @Override
        public ObjectFactoryInstance withoutFieldInjection() {
            recipe.disallow(Option.FIELD_INJECTION);
            return this;
        }

        @Override
        public ObjectFactoryInstance ignoreUnknownProperties() {
            recipe.allow(Option.IGNORE_MISSING_PROPERTIES);
            return this;
        }

        @Override
        public ObjectFactoryInstance withProperties(final Map<String, ?> map) {
            if (recipe.getOptions().contains(Option.FIELD_INJECTION)
                    && recipe.getOptions().contains(Option.PRIVATE_PROPERTIES)) {
                map.forEach(recipe::setFieldProperty);
            } else {
                recipe.setAllProperties(map);
            }
            return this;
        }

        @Override
        public <T> T create(final Class<T> aClass) {
            if (recipe
                    .getProperties()
                    .keySet()
                    .stream()
                    .map(it -> it.toLowerCase(ROOT))
                    .collect(toSet())
                    .size() == recipe.getProperties().size()) {
                recipe.allow(Option.CASE_INSENSITIVE_PROPERTIES);
            }
            try {
                return aClass.cast(recipe.create(Thread.currentThread().getContextClassLoader()));
            } catch (final RuntimeException re) {
                throw new IllegalArgumentException(re);
            }
        }
    }
}
