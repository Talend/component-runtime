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
package org.talend.sdk.component.form.model;

import static java.util.Collections.emptyMap;

import java.util.Map;

public class UiActionResult {

    private String error;

    private Map<String, String> errors;

    private String formName;

    private String schema;

    private String value;

    private Map<String, Object> rawData;

    public Map<String, String> getErrors() {
        return errors == null ? emptyMap() : errors;
    }

    public void setErrors(final Map<String, String> errors) {
        this.errors = errors;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public void setRawData(final Map<String, Object> rawData) {
        this.rawData = rawData;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(final String formName) {
        this.formName = formName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
