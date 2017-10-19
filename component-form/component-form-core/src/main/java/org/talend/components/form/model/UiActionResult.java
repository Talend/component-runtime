// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.form.model;

import static java.util.Collections.emptyMap;

import java.util.Map;

public class UiActionResult {

    private UpdateType type;

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

    public UpdateType getType() {
        return type;
    }

    public void setType(final UpdateType type) {
        this.type = type;
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

    public enum UpdateType {
        TF_UPDATE_FORM_DATA,
        TF_SET_ALL_ERRORS,
        TF_SET_PARTIAL_ERROR,
        TF_CREATE_FORM,
        TF_REMOVE_FORM,
        TF_UPDATE_FORM
    }
}
