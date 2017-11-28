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
import static org.talend.core.model.process.EParameterFieldType.HIDDEN_TEXT;
import static org.talend.core.model.process.EParameterFieldType.MEMO_JAVA;
import static org.talend.core.model.process.EParameterFieldType.OPENED_LIST;
import static org.talend.core.model.process.EParameterFieldType.TABLE;
import static org.talend.core.model.process.EParameterFieldType.TEXT;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.ACTION_DYNAMIC_VALUES;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.JAVA;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_CODE;
import static org.talend.sdk.component.studio.model.parameter.Metadatas.UI_CREDENTIAL;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.BOOLEAN;
import static org.talend.sdk.component.studio.model.parameter.PropertyTypes.STRING;

import org.talend.core.model.process.EParameterFieldType;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.AllArgsConstructor;

/**
 * Maps metadata retrieved from {@link SimplePropertyDefinition} to {@link EParameterFieldType}
 */
@AllArgsConstructor
public class WidgetTypeMapper {

    private final SimplePropertyDefinition property;
    
    /**
     * Recognizes {@link EParameterFieldType} for given {@link SimplePropertyDefinition}
     * Implementation note: Most possible types are located first.
     * All checks are implemented in separate methods
     * Only one checker method returns <code>true<code> for particular Property Definition
     * 
     * @return widget type
     */
    public EParameterFieldType getFieldType() {
        if (isText()) {
            return TEXT;
        } else if (isHiddenText()) {
            return HIDDEN_TEXT;
        } else if (isCheck()) {
            return CHECK;
        } else if (isClosedList()) {
            return CLOSED_LIST;
        } else if (isOpenedList()) {
            return OPENED_LIST;
        } else if (isFile()) {
            return FILE;
        } else if (isTable()) {
            return TABLE;
        } else if (isMemoJava()) {
            return MEMO_JAVA;
        }
        return TEXT;
    }
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#TEXT} 
     */
    private boolean isText() {
        return STRING.equals(property.getType()) && property.getMetadata().isEmpty();
    }
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#HIDDEN_TEXT} 
     */
    private boolean isHiddenText() {
        return STRING.equals(property.getType()) && property.getMetadata().containsKey(UI_CREDENTIAL);
    }
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#CHECK}
     */
    private boolean isCheck() {
        return BOOLEAN.equals(property.getType());
    }
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#CLOSED_LIST}
     */
    private boolean isClosedList() {
        return property.getMetadata().containsKey(ACTION_DYNAMIC_VALUES);
    }
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#OPENED_LIST}
     * TODO
     */
    private boolean isOpenedList() {
        return false;
    }
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#FILE}
     * TODO decide and implement it
     */
    private boolean isFile() {
        return false;
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
    
    /**
     * Checks whether widget type is {@link EParameterFieldType#TABLE}
     * TODO implement it
     */
    private boolean isTable() {
        return false;
    }
}
