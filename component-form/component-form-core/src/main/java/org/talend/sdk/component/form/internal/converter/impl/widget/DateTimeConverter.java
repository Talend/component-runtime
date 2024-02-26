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
package org.talend.sdk.component.form.internal.converter.impl.widget;

import static java.util.Collections.singletonMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class DateTimeConverter extends AbstractWidgetConverter {

    private final String format; // date, time, datetime, zoneddatetime

    public DateTimeConverter(final Collection<UiSchema> schemas, final Collection<SimplePropertyDefinition> properties,
            final Collection<ActionReference> actions, final JsonSchema jsonSchema, final String lang,
            final String format) {
        super(schemas, properties, actions, jsonSchema, lang);
        this.format = format;
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenApply(context -> {
            final UiSchema schema = newUiSchema(context);
            initDatePicker(context, schema);
            return context;
        });
    }

    private void initDatePicker(final PropertyContext<?> context, final UiSchema schema) {
        final String dateFormat =
                context.getProperty().getMetadata().getOrDefault("ui::datetime::dateFormat", "YYYY/MM/DD");
        final boolean useSeconds = Boolean
                .parseBoolean(context.getProperty().getMetadata().getOrDefault("ui::datetime::useSeconds", "true"));
        final boolean useUTC =
                Boolean.parseBoolean(context.getProperty().getMetadata().getOrDefault("ui::datetime::useUTC", "true"));
        switch (format) {
        case "time": {
            schema.setWidget("datetime"); // todo: move to "time" widget when existing
            final Map<String, Object> options = new HashMap<>();
            options.put("useSeconds", useSeconds);
            schema.setOptions(options);
            break;
        }
        case "date":
            schema.setWidget("date");
            schema.setOptions(singletonMap("dateFormat", dateFormat));
            break;
        case "datetime":
        case "zoneddatetime": {
            schema.setWidget("datetime");
            final Map<String, Object> options = new HashMap<>();
            options.put("useSeconds", useSeconds);
            options.put("useUTC", useUTC);
            options.put("dateFormat", dateFormat);
            schema.setOptions(options);
            break;
        }
        default:
            throw new IllegalArgumentException(
                    "Unsupported date format: '" + format + "' on '" + context.getProperty().getPath() + "'");
        }
    }
}
