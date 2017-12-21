/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.parameter;

import static org.talend.core.model.process.EParameterFieldType.CHECK;
import static org.talend.core.model.process.EParameterFieldType.CLOSED_LIST;
import static org.talend.core.model.process.EParameterFieldType.FILE;
import static org.talend.core.model.process.EParameterFieldType.MEMO_JAVA;
import static org.talend.core.model.process.EParameterFieldType.OPENED_LIST;
import static org.talend.core.model.process.EParameterFieldType.PASSWORD;
import static org.talend.core.model.process.EParameterFieldType.TABLE;
import static org.talend.core.model.process.EParameterFieldType.TEXT;
import static org.talend.core.model.process.EParameterFieldType.TEXT_AREA;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.JAVA;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_CODE;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_CREDENTIAL;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_TEXTAREA;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.ARRAY;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.BOOLEAN;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.ENUM;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.STRING;

import org.talend.core.model.process.EParameterFieldType;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

/**
 * Maps metadata retrieved from {@link SimplePropertyDefinition} to {@link EParameterFieldType}
 */
public class WidgetTypeMapper {

    private SimplePropertyDefinition property;

    /**
     * Recognizes {@link EParameterFieldType} for given {@link SimplePropertyDefinition}
     * Implementation note: Most possible types are located first.
     * All checks are implemented in separate methods
     * Only one checker method returns {@code true} for particular Property Definition
     * 
     * @param property Property, which field type should be defined
     * @return widget type
     */
    public EParameterFieldType getFieldType(final SimplePropertyDefinition property) {
        if (property == null) {
            throw new IllegalArgumentException("property should not be null");
        }
        this.property = property;
        if (isText()) {
            return getTextType();
        } else if (isCredential()) {
            return getCredentialType();
        } else if (isTextArea()) {
            return getTextAreaType();
        } else if (isCheck()) {
            return getCheckType();
        } else if (isClosedList()) {
            return getClosedListType();
        } else if (isOpenedList()) {
            return getOpenedListType();
        } else if (isFile()) {
            return getFileType();
        } else if (isTable()) {
            return getTableType();
        } else if (isMemoJava()) {
            return getMemoJavaType();
        }
        return getTextType();
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#TEXT}
     */
    private boolean isText() {
        return STRING.equals(property.getType()) && property.getMetadata().isEmpty();
    }

    protected EParameterFieldType getTextType() {
        return TEXT;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#TEXT_AREA}
     */
    private boolean isTextArea() {
        return property.getMetadata().containsKey(UI_TEXTAREA);
    }

    protected EParameterFieldType getTextAreaType() {
        return TEXT_AREA;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#PASSWORD}
     */
    private boolean isCredential() {
        return property.getMetadata().containsKey(UI_CREDENTIAL);
    }

    protected EParameterFieldType getCredentialType() {
        return PASSWORD;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#CHECK}
     */
    private boolean isCheck() {
        return BOOLEAN.equals(property.getType());
    }

    protected EParameterFieldType getCheckType() {
        return CHECK;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#CLOSED_LIST}
     */
    private boolean isClosedList() {
        return ENUM.equals(property.getType());
    }

    protected EParameterFieldType getClosedListType() {
        return CLOSED_LIST;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#OPENED_LIST}
     * TODO
     */
    private boolean isOpenedList() {
        return false;
    }

    protected EParameterFieldType getOpenedListType() {
        return OPENED_LIST;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#FILE}
     * TODO decide and implement it
     */
    private boolean isFile() {
        return false;
    }

    protected EParameterFieldType getFileType() {
        return FILE;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#MEMO_JAVA}
     */
    private boolean isMemoJava() {
        if (property.getMetadata().get(UI_CODE) == null) {
            return false;
        }
        return JAVA.equals(property.getMetadata().get(UI_CODE));
    }

    protected EParameterFieldType getMemoJavaType() {
        return MEMO_JAVA;
    }

    /**
     * Checks whether widget type is {@link EParameterFieldType#TABLE}
     */
    private boolean isTable() {
        return ARRAY.equals(property.getType());
    }

    protected EParameterFieldType getTableType() {
        return TABLE;
    }
}
