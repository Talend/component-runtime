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

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.IElement;
import org.talend.designer.core.model.components.ElementParameter;

/**
 * Adds schema property on Basic view in the 1st row by default
 */
public class SchemaSettingsCreator extends SettingsCreator {

    /**
     * Creates instance and adds schema property
     *
     * @param iNode Node for which parameters are created
     * @param category Parameter category
     * @param redrawParameter Parameter which defines whether UI should be redrawn
     */
    public SchemaSettingsCreator(final IElement iNode, final EComponentCategory category,
            final ElementParameter redrawParameter) {
        super(iNode, category, redrawParameter);
    }
}
