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

import java.util.List;
import java.util.Map;

import org.talend.core.model.process.IElement;

/**
 * Represents Table parameter. Table parameter is ElementParameter, which EParameterFieldType is TABLE
 * Value of Table parameter should have List<Map<String, Object>> type. Its value is a list of table records.
 * Each table record is represented by Map.
 * This class ensures Table parameter integrity. When user sets null or "" value to this parameter, empty list is set
 * instead. Also it implements convertion of stored value from and to String representation, which is used to serialize
 * parameter
 * in repository
 */
public class TableElementParameter extends TaCoKitElementParameter {

    public TableElementParameter(final IElement element) {
        super(element);
    }

    /**
     * Retrieves stored value and converts it to String using {@link List#toString()} method
     * 
     * @return string representation of stored value
     */
    public String getStringValue() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tableValue = (List<Map<String, Object>>) super.getValue();
        return tableValue.toString();
    }

    /**
     * Converts value from string to List of Maps (Table) and delegates parent to set the value.
     * Expected input string is as follows: "[{key1=value11, key2=value12}, {key1=value21, key2=value22}]"
     * A Map instance in the List represents Table row. Rows are separated by ", " (comma with a whitespace).
     * Entries in the Map are also should be separated by ", ".
     * Generally, input string should be equal to the result of a call to List.toString().
     * 
     * @param newValue value to be set
     */
    @Override
    public void setStringValue(final String newValue) {
        super.setValue(ValueConverter.toTable(newValue));
    }

}
