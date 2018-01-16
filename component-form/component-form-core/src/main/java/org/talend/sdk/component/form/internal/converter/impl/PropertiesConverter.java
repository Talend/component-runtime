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
package org.talend.sdk.component.form.internal.converter.impl;

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class PropertiesConverter implements PropertyConverter {

    private final Map<String, Object> defaults;

    private final Collection<SimplePropertyDefinition> properties;

    public PropertiesConverter(final Map<String, Object> defaults,
            final Collection<SimplePropertyDefinition> properties) {
        this.defaults = defaults;
        this.properties = properties;
    }

    @Override
    public void convert(final PropertyContext context) {
        final SimplePropertyDefinition property = context.getProperty();
        if ("object".equalsIgnoreCase(property.getType())) {
            final Map<String, Object> childDefaults = new HashMap<>();
            defaults.put(property.getName(), childDefaults);
            final PropertiesConverter propertiesConverter = new PropertiesConverter(childDefaults, properties);
            properties.stream().filter(context::isDirectChild).map(PropertyContext::new).forEach(
                    propertiesConverter::convert);
            return;
        }

        ofNullable(property.getMetadata().getOrDefault("ui::defaultvalue::value", property.getDefaultValue()))
                .ifPresent(value -> {
                    if ("number".equalsIgnoreCase(property.getType())) {
                        defaults.put(property.getName(), Double.parseDouble(value));
                    } else if ("boolean".equalsIgnoreCase(property.getType())) {
                        defaults.put(property.getName(), Boolean.parseBoolean(value));
                    } else {
                        defaults.put(property.getName(), value);
                    }
                });
    }
}
