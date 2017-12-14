/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.metadata;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.talend.core.model.process.IElement;
import org.talend.designer.core.model.components.ElementParameter;

import lombok.Getter;
import lombok.Setter;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitElementParameter extends ElementParameter {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final List<IValueChangedListener> valueChangeListeners = new ArrayList<>();

    @Setter
    @Getter
    private boolean isForceRefresh = false;

    public TaCoKitElementParameter(final IElement element) {
        super(element);
        setTaggedValue("org.talend.sdk.component.source", "tacokit");
    }

    @Override
    public void setValue(final Object newValue) {
        Object oldValue = super.getValue();
        super.setValue(newValue);
        firePropertyChange(ITaCoKitElementParameterEventProperties.EVENT_PROPERTY_VALUE_CHANGED, oldValue, newValue);
        fireValueChange(oldValue, newValue);
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void registerListener(final String propertyName, final PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public void unregisterListener(final String propertyName, final PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    private boolean hasPropertyChangeListener() {
        return pcs.getPropertyChangeListeners().length != 0;
    }

    public void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        if (hasPropertyChangeListener()) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void addValueChangeListener(final IValueChangedListener listener) {
        valueChangeListeners.add(listener);
    }

    public boolean removeValueChangeListener(final IValueChangedListener listener) {
        return valueChangeListeners.remove(listener);
    }

    public void fireValueChange(final Object oldValue, final Object newValue) {
        for (IValueChangedListener listener : valueChangeListeners) {
            listener.onValueChanged(this, oldValue, newValue);
        }
    }

    /**
     * Computes index of specified <code>item</code> either in itemsDisplayCodeName or itemsDisplayCodeName
     * super class fields
     * This overridden implementation fixes an error, when <code>item</code> wasn't found in both arrays.
     * It returns 0 in such case instead of -1. -1 causes ArrayIndexOutOfBoundsException, when new table column is added
     * 
     * @param item default closed list value
     * @return default value index in possible values array
     */
    @Override
    public int getIndexOfItemFromList(final String item) {
        int index = super.getIndexOfItemFromList(item);
        if (index == -1) {
            return 0;
        }
        return index;
    }

    public interface IValueChangedListener {

        void onValueChanged(final TaCoKitElementParameter elementParameter, final Object oldValue,
                final Object newValue);
    }

}
