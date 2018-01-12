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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class UiPropertiesConverter implements PropertyConverter {

    private final Map<String, Object> defaults;

    private final Collection<SimplePropertyDefinition> properties;

    public UiPropertiesConverter(final Map<String, Object> defaults,
            final Collection<SimplePropertyDefinition> properties) {
        this.defaults = defaults;
        this.properties = properties;
    }

    @Override
    public void convert(final PropertyContext p) {
        if ("object".equalsIgnoreCase(p.getProperty().getType())) {
            final Map<String, Object> childDefaults = new HashMap<>();
            defaults.put(p.getProperty().getName(), childDefaults);
            final UiPropertiesConverter uiPropertiesConverter = new UiPropertiesConverter(childDefaults, properties);
            properties.stream().filter(p::isDirectChild).map(PropertyContext::new).forEach(
                    uiPropertiesConverter::convert);
        } else if (p.getProperty().getMetadata().containsKey("ui::defaultvalue::value")) {
            final String def = p.getProperty().getMetadata().get("ui::defaultvalue::value");
            if ("number".equalsIgnoreCase(p.getProperty().getType())) {
                defaults.put(p.getProperty().getName(), Double.parseDouble(def));
            } else if ("boolean".equalsIgnoreCase(p.getProperty().getType())) {
                defaults.put(p.getProperty().getName(), Boolean.parseBoolean(def));
            } else {
                defaults.put(p.getProperty().getName(), def);
            }
        }
    }
}
