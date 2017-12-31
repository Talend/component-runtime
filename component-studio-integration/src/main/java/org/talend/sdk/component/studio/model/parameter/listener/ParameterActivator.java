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
package org.talend.sdk.component.studio.model.parameter.listener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.talend.core.model.process.IElementParameter;

import lombok.RequiredArgsConstructor;

/**
 * {@link PropertyChangeListener}, which activates/deactivates {@link IElementParameter} according target
 * {@link IElementParameter} new value
 * If new value is the same as one of specified <code>values</code>, then {@link ParameterActivator} activates (shows)
 * {@link IElementParameter}.
 * Else it deactivates {@link IElementParameter}
 */
@RequiredArgsConstructor
public class ParameterActivator implements PropertyChangeListener {

    private final String[] values;

    private final IElementParameter parameter;

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (test(event.getNewValue())) {
            parameter.setShow(true);
        } else {
            parameter.setShow(false);
        }
    }

    boolean test(final Object newValue) {
        for (String value : values) {
            if (value.equals(String.valueOf(newValue))) {
                return true;
            }
        }
        return false;
    }

}
