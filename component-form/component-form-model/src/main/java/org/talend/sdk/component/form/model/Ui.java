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
package org.talend.sdk.component.form.model;

import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.Collection;

import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Ui {

    private JsonSchema jsonSchema;

    private Collection<UiSchema> uiSchema;

    private Object properties;

    public static Builder ui() {
        return new Builder();
    }

    @NoArgsConstructor(access = PRIVATE)
    public static final class Builder {

        private JsonSchema jsonSchema;

        private Collection<UiSchema> uiSchema;

        private Object properties;

        public Builder withJsonSchema(final JsonSchema jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public Builder withUiSchema(final UiSchema uiSchema) {
            if (this.uiSchema == null) {
                this.uiSchema = new ArrayList<>();
            }
            this.uiSchema.add(uiSchema);
            return this;
        }

        public Builder withUiSchema(final Collection<UiSchema> uiSchema) {
            if (this.uiSchema == null) {
                this.uiSchema = new ArrayList<>();
            }
            this.uiSchema.addAll(uiSchema);
            return this;
        }

        public <T> Builder withProperties(final T properties) {
            this.properties = properties;
            return this;
        }

        public Ui build() {
            final Ui ui = new Ui();
            ui.setJsonSchema(jsonSchema);
            ui.setUiSchema(uiSchema);
            ui.setProperties(properties);
            return ui;
        }
    }
}
