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
package org.talend.sdk.component.form.internal.converter.impl.schema;

import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyConverter;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ArrayPropertyConverter implements PropertyConverter {

    private final JsonSchema jsonSchema;

    private final Collection<SimplePropertyDefinition> properties;

    @Override
    public void convert(final PropertyContext p) {
        jsonSchema.setType(p.getProperty().getType().toLowerCase(ROOT));
        final String prefix = p.getProperty().getPath() + "[]";
        final List<SimplePropertyDefinition> arrayElements =
                properties.stream().filter(child -> child.getPath().startsWith(prefix)).collect(toList());

        if (arrayElements.stream().anyMatch(e -> e.getPath().startsWith(prefix + '.'))) {// complex object
            final JsonSchema items = new JsonSchema();
            items.setProperties(new HashMap<>());
            arrayElements.stream().map(PropertyContext::new).forEach(
                    e -> new JsonSchemaConverter(items, emptyList()).convert(e));
            jsonSchema.setItems(items);
        } else if (!arrayElements.isEmpty()) {// primitive
            final String type = arrayElements.get(0).getType();
            final JsonSchema item = new JsonSchema();
            item.setTitle(jsonSchema.getTitle());
            item.setType("enum".equalsIgnoreCase(type) ? "string" : type.toLowerCase(ROOT));
            jsonSchema.setItems(item);
        }
    }
}
