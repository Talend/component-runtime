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
package org.talend.sdk.component.studio.ui.wizard.page;

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElement;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;
import org.talend.sdk.component.studio.model.parameter.PropertyNode;
import org.talend.sdk.component.studio.model.parameter.SettingsCreator;

/**
 * Provides quick fix for password display issue
 */
public class WizardSettingsCreator extends SettingsCreator {

    public WizardSettingsCreator(final IElement iNode, final EComponentCategory category) {
        super(iNode, category);
    }

    /**
     * Creates {@link TaCoKitElementParameter} for Password field type
     * Sets Hidden text field type instead of Password
     */
    @Override
    protected TaCoKitElementParameter visitPassword(final PropertyNode node) {
        TaCoKitElementParameter parameter = createParameter(node);
        parameter.setFieldType(EParameterFieldType.HIDDEN_TEXT);
        return parameter;
    }

}
