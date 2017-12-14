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
package org.talend.sdk.component.studio.ui.composite;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.gmf.util.DisplayUtils;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.ui.views.properties.composites.MissingSettingsMultiThreadDynamicComposite;
import org.talend.sdk.component.studio.metadata.ITaCoKitElementParameterEventProperties;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter.IValueChangedListener;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel;
import org.talend.sdk.component.studio.metadata.model.TaCoKitConfigurationModel.ValueModel;
import org.talend.sdk.component.studio.util.TaCoKitUtil;

public class TaCoKitWizardComposite extends MissingSettingsMultiThreadDynamicComposite implements PropertyChangeListener {

    private List<PropertyChangeListener> propertyChangeListeners;

    private Element element;

    private IValueChangedListener valueChangedListener;

    private List<IValueChangedListener> externalValueChangedListener = new ArrayList<>();

    private TaCoKitConfigurationModel configurationModel;

    public TaCoKitWizardComposite(final Composite parentComposite, final int styles, final EComponentCategory section,
            final Element element, final TaCoKitConfigurationModel model, final boolean isCompactView,
            final Color backgroundColor) {
        super(parentComposite, styles, section, element, isCompactView, backgroundColor);
        this.element = element;
        this.configurationModel = model;
        propertyChangeListeners = new ArrayList<>();
        valueChangedListener = new ValueChangedListener();
        init();
    }

    private void init() {
        Iterator<? extends IElementParameter> iter = this.element.getElementParameters().iterator();
        while (iter.hasNext()) {
            try {
                IElementParameter elementParameter = iter.next();
                String key = elementParameter.getName();
                ValueModel valueModel = configurationModel.getValue(key);
                if (valueModel != null) {
                    if (valueModel.getConfigurationModel() != configurationModel) {
                        elementParameter.setReadOnly(true);
                        // update value from parent
                        elementParameter.setValue(valueModel.getValue());
                    }
                    elementParameter.setValue(valueModel.getValue());
                }
                if (elementParameter instanceof TaCoKitElementParameter) {
                    TaCoKitElementParameter tacokitParameter = (TaCoKitElementParameter) elementParameter;
                    tacokitParameter.addPropertyChangeListener(this);
                    tacokitParameter.addValueChangeListener(valueChangedListener);
                }
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (TaCoKitUtil.equals(ITaCoKitElementParameterEventProperties.EVENT_PROPERTY_VALUE_CHANGED, propertyName)) {
            refresh();
        }
        onPropertyChanged(event);
    }

    @Override
    public void refresh() {
        if (element instanceof FakeElement) {
            DisplayUtils.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    operationInThread();
                }
            });
        } else {
            super.refresh();
        }
    }

    @Override
    public int getMinHeight() {
        if (minHeight < 200) {
            return 200;
        } else if (minHeight > 700) {
            return 700;
        }
        return minHeight;
    }

    private void onPropertyChanged(final PropertyChangeEvent event) {
        for (PropertyChangeListener listener : propertyChangeListeners) {
            listener.propertyChange(event);
        }
    }

    public boolean addPropertyChangeListener(final PropertyChangeListener listener) {
        return this.propertyChangeListeners.add(listener);
    }

    public boolean removePropertyChangeListener(final PropertyChangeListener listener) {
        return this.propertyChangeListeners.remove(listener);
    }

    public boolean addValueChangedListener(final IValueChangedListener listener) {
        return this.externalValueChangedListener.add(listener);
    }

    public boolean removeValueChangedListener(final IValueChangedListener listener) {
        return this.externalValueChangedListener.remove(listener);
    }

    private class ValueChangedListener implements IValueChangedListener {

        @Override
        public void onValueChanged(final TaCoKitElementParameter elementParameter, final Object oldValue,
                final Object newValue) {
            for (IValueChangedListener listener : externalValueChangedListener) {
                listener.onValueChanged(elementParameter, oldValue, newValue);
            }
        }

    }

}
