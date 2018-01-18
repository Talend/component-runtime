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

import org.talend.core.model.process.EParameterFieldType;
import org.talend.sdk.component.studio.model.parameter.ValidationLabel;

/**
 * Base class for Property Validators. It validates new value. If validation is not passed, then
 * it activates {@code label} ElementParameter and sets its validation message
 */
public abstract class PropertyValidator implements PropertyChangeListener {

    private final ValidationLabel label;

    private final String validationMessage;

    PropertyValidator(final ValidationLabel label, final String validationMessage) {
        if (label.getFieldType() != EParameterFieldType.LABEL) {
            throw new IllegalArgumentException("parameter should be a LABEL");
        }
        this.label = label;
        this.validationMessage = validationMessage;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (!validate(event.getNewValue())) {
            label.show(validationMessage);
        } else {
            label.hide(validationMessage);
        }
    }

    abstract boolean validate(final Object newValue);

}
