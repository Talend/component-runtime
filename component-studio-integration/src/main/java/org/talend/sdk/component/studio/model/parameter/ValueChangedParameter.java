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

import org.talend.core.model.process.IElement;

/**
 * Fires parameter change events directly after value changed event
 */
public class ValueChangedParameter extends TaCoKitElementParameter {

    public ValueChangedParameter(final IElement element) {
        super(element);
    }

    /**
     * Sets parameter value and fires parameter change event, which is handled by registered listeners.
     * Subclasses should extend (override and call super.setValue()) this method to provide correct conversion, when
     * they use other value type than String.
     *
     * @param newValue value to be set
     */
    @Override
    public void setValue(final Object newValue) {
        final Object oldValue = super.getValue();
        super.setValue(newValue);
        firePropertyChange(getName(), oldValue, newValue);
        fireValueChange(oldValue, newValue);
        redraw();
    }
}
