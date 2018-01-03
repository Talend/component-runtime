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
package org.talend.sdk.component.studio.model.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.talend.core.model.process.IElement;

/**
 * Represents Table parameter. Table parameter is ElementParameter, which EParameterFieldType is TABLE
 * Value of Table parameter should have List<Map<String, Object>> type. Its value is a list of table records.
 * Each table record is represented by Map.
 * This class ensures Table parameter integrity. When user sets null or "" value to this parameter, empty list is set
 * instead
 */
public class TableElementParameter extends TaCoKitElementParameter {

    public TableElementParameter(final IElement element) {
        super(element);
    }

    /**
     * Sets value of this Table parameter. Expected type of value is List<Map<String, Object>>.
     * null and "" are also accepted. In this case empty list will be set as value
     * 
     * @param newValue new value to set
     */
    @Override
    public void setValue(final Object newValue) {
        if (newValue == null || "".equals(newValue)) {
            super.setValue(new ArrayList<Map<String, Object>>());
        } else if (!(newValue instanceof List)) {
            throw new IllegalArgumentException("newValue should be of List<Map<String, Object>> type");
        } else {
            super.setValue(newValue);
        }
    }

}
