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
 * Value of Table parameter should have {@code List<Map<String, Object>>} type. Its value is a list of table records.
 * Each table record is represented by Map.
 * This class ensures Table parameter integrity. When user sets string value to this parameter, it is converted to List
 * Also it implements conversion of stored value from String representation in getStringValue() method. It is used to
 * serialize
 * parameter value in repository.
 */
public class TableElementParameter extends ValueChangedParameter {

    public TableElementParameter(final IElement element) {
        super(element);
    }

    /**
     * Retrieves stored value and converts it to String using List.toString() method
     * 
     * @return string representation of stored value
     */
    @Override
    public String getStringValue() {
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> tableValue = (List<Map<String, Object>>) super.getValue();
        return tableValue.toString();
    }

    /**
     * Sets new parameter value. If new value is of type String, converts it to List of Maps (Table) and delegates
     * parent to set the value.
     * Expected string input should be as follows: "[{key1=value11, key2=value12}, {key1=value21, key2=value22}]"
     * A Map instance in the List represents Table row. Rows are separated by ", " (comma with a whitespace).
     * Entries in the Map are also should be separated by ", ".
     * Generally, string argument should be equal to the result of a call to List.toString().
     * 
     * If incoming argument is of type List, then sets it without conversion.
     * Else it throws exception.
     * 
     * @param newValue value to be set
     */
    @Override
    public void setValue(final Object newValue) {
        if (newValue == null || newValue instanceof String) {
            super.setValue(ValueConverter.toTable((String) newValue));
        } else if (newValue instanceof List) {
            super.setValue(newValue);
        } else {
            throw new IllegalArgumentException("wrong type on new value: " + newValue.getClass().getName());
        }
    }

}
