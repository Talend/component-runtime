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
package org.talend.sdk.component.form.internal.converter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.converter.impl.widget.AbstractWidgetConverter;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Data;

/**
 * Ensure to implement one of the convert methods.
 */
public interface CustomPropertyConverter extends PropertyConverter {

    boolean supports(PropertyContext<?> context);

    @Override
    @Deprecated // use the other convert method
    default CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> context) {
        return convert(context, null);
    }

    default CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> context,
            final ConverterContext converterContext) {
        return convert(context);
    }

    @Data
    class ConverterContext {

        private final String family;

        private final Collection<UiSchema> schemas;

        private final Collection<SimplePropertyDefinition> includedProperties;

        private final Client client;

        private final JsonSchema jsonSchema;

        private final Collection<SimplePropertyDefinition> properties;

        private final Collection<ActionReference> actions;

        private final String lang;

        private Tools tools;

        public <T> CompletionStage<List<UiSchema.NameValue>> loadDynamicValues(final Client<T> client,
                final String family, final String actionName, final T context) {
            return getTools().loadDynamicValues(client, family, actionName, context);
        }

        public UiSchema.Trigger toTrigger(final Collection<SimplePropertyDefinition> properties,
                final SimplePropertyDefinition prop, final ActionReference ref) {
            return getTools().toTrigger(properties, prop, ref);
        }

        public List<UiSchema.Parameter> toParams(final Collection<SimplePropertyDefinition> properties,
                final SimplePropertyDefinition prop, final ActionReference ref, final String parameters) {
            return getTools().toParams(properties, prop, ref, parameters);
        }

        public UiSchema newUiSchema(final PropertyContext<?> ctx) {
            return getTools().newUiSchema(ctx);
        }

        public UiSchema newOrphanSchema(final PropertyContext<?> ctx) {
            return getTools().newOrphanSchema(ctx);
        }

        public Map<String, Collection<Object>> createCondition(final PropertyContext<?> ctx) {
            return getTools().createCondition(ctx);
        }

        public JsonSchema findJsonSchema(final PropertyContext<?> cs) {
            return getTools().findJsonSchema(cs);
        }

        private Tools getTools() {
            if (tools == null) {
                tools = new Tools(schemas, properties, actions, jsonSchema, lang) {

                    @Override
                    public CompletionStage<PropertyContext<?>>
                            convert(final CompletionStage<PropertyContext<?>> context) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            return tools;
        }

        private static class Tools extends AbstractWidgetConverter {

            private Tools(final Collection<UiSchema> schemas, final Collection<SimplePropertyDefinition> properties,
                    final Collection<ActionReference> actions, final JsonSchema jsonSchema, final String lang) {
                super(schemas, properties, actions, jsonSchema, lang);
            }

            @Override
            public <T> CompletionStage<List<UiSchema.NameValue>> loadDynamicValues(final Client<T> client,
                    final String family, final String actionName, final T context) {
                return super.loadDynamicValues(client, family, actionName, context);
            }

            @Override
            public UiSchema.Trigger toTrigger(final Collection<SimplePropertyDefinition> properties,
                    final SimplePropertyDefinition prop, final ActionReference ref) {
                return super.toTrigger(properties, prop, ref);
            }

            @Override
            public List<UiSchema.Parameter> toParams(final Collection<SimplePropertyDefinition> properties,
                    final SimplePropertyDefinition prop, final ActionReference ref, final String parameters) {
                return super.toParams(properties, prop, ref, parameters);
            }

            @Override
            public UiSchema newUiSchema(final PropertyContext<?> ctx) {
                return super.newUiSchema(ctx);
            }

            @Override
            public UiSchema newOrphanSchema(final PropertyContext<?> ctx) {
                return super.newOrphanSchema(ctx);
            }

            @Override
            public Map<String, Collection<Object>> createCondition(final PropertyContext<?> ctx) {
                return super.createCondition(ctx);
            }

            @Override
            public JsonSchema findJsonSchema(final PropertyContext<?> cs) {
                return super.findJsonSchema(cs);
            }

            @Override
            public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> context) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
