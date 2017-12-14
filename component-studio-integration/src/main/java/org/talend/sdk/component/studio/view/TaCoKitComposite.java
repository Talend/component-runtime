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
package org.talend.sdk.component.studio.view;

import static org.talend.sdk.component.studio.metadata.ITaCoKitElementParameterEventProperties.EVENT_PROPERTY_VALUE_CHANGED;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.talend.commons.ui.gmf.util.DisplayUtils;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.ui.views.properties.composites.MissingSettingsMultiThreadDynamicComposite;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;

/**
 * Registers PropertyChangeListener for each IElementParameter during instantiation
 * PropertyChangeListener refreshes layout after each IElementParameter value update
 */
// FIXME probably wrong parent class
public class TaCoKitComposite extends MissingSettingsMultiThreadDynamicComposite {

    public TaCoKitComposite(final Composite parentComposite, final int styles, final EComponentCategory section,
            final Element element, final boolean isCompactView, final Color backgroundColor) {
        super(parentComposite, styles, section, element, isCompactView, backgroundColor);
        init();
    }

    /**
     * For each {@link TaCoKitElementParameter} registers PropertyChangeListener, which calls
     * {@link this#refresh()} on each {@link TaCoKitElementParameter} value change event
     */
    private void init() {
        elem
                .getElementParameters()
                .stream()
                .filter(p -> p instanceof TaCoKitElementParameter)
                .map(p -> (TaCoKitElementParameter) p)
                .forEach(p -> p.addPropertyChangeListener(EVENT_PROPERTY_VALUE_CHANGED, event -> refresh()));
    }

    // TODO is it required?
    @Override
    public void refresh() {
        if (elem instanceof FakeElement) { // sync exec
            DisplayUtils.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    operationInThread();
                }
            });
        } else { // async exec
            super.refresh();
        }
    }

}
