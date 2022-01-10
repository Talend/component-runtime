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
package org.talend.sdk.component.runtime.di.schema;

import java.util.Map;

import lombok.Data;

// plain copy of studio MetadataColumn class,
// we don't support it all yet but it express the potentially enhancement on our Schema model
@Data
public class Column {

    private String id;

    private String label;

    private Boolean key;

    private String sourceType;

    private String talendType;

    private Boolean nullable;

    private Integer length = 0;

    private Integer precision = 0;

    private String defaut;

    private String comment;

    private Integer originalLength;

    private String pattern;

    private Boolean custom;

    private Boolean readOnly;

    private Integer customId;

    private String originalDbColumnName;

    private String relatedEntity;

    private String relationshipType;

    private String expression;

    private Boolean usefulColumn;

    private Map<String, String> additionalField;

    public String getDefault() {
        return defaut;
    }

    public void setDefault(final String value) {
        defaut = value;
    }
}
