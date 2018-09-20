/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

    private boolean key;

    private String sourceType;

    private String talendType;

    private boolean nullable;

    private Integer length;

    private Integer precision;

    private String defaut;

    private String comment;

    private Integer originalLength;

    private String pattern;

    private boolean custom;

    private boolean readOnly;

    private int customId;

    private String originalDbColumnName;

    private String relatedEntity;

    private String relationshipType;

    private String expression;

    private boolean usefulColumn;

    private Map<String, String> additionalField;
}
