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
package org.talend.sdk.component.studio.model.parameter;

import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IElement;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.sdk.component.studio.metadata.TaCoKitElementParameter;

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
        if (category == EComponentCategory.BASIC) {
            addSchemaProperty();
        }
    }

    /**
     * FIXME
     */
    private void addSchemaProperty() {
        TaCoKitElementParameter schema = new TaCoKitElementParameter(getNode());
        schema.setName("SCHEMA");
        schema.setDisplayName("!!!SCHEMA.NAME!!!");
        schema.setCategory(EComponentCategory.BASIC);
        schema.setFieldType(EParameterFieldType.SCHEMA_TYPE);
        schema.setNumRow(1);
        schema.setShow(true);
        schema.setReadOnly(false);
        schema.setRequired(true);
        schema.setContext(EConnectionType.FLOW_MAIN.getName());

        // add child parameters
        // defines whether schema is built-in or repository
        TaCoKitElementParameter childParameter1 = new TaCoKitElementParameter(getNode());
        childParameter1.setCategory(EComponentCategory.BASIC);
        childParameter1.setContext(EConnectionType.FLOW_MAIN.getName());
        childParameter1.setDisplayName("Schema");
        childParameter1.setFieldType(EParameterFieldType.TECHNICAL);
        childParameter1.setListItemsDisplayCodeName(new String[] { "BUILT_IN", "REPOSITORY" });
        childParameter1.setListItemsDisplayName(new String[] { "Built-In", "Repository" });
        childParameter1.setListItemsValue(new String[] { "BUILT_IN", "REPOSITORY" });
        childParameter1.setName(EParameterName.SCHEMA_TYPE.getName());
        childParameter1.setNumRow(1);
        childParameter1.setParentParameter(schema);
        childParameter1.setShow(true);
        childParameter1.setShowIf("SCHEMA =='REPOSITORY'");
        childParameter1.setValue("BUILT_IN");
        schema.getChildParameters().put(EParameterName.SCHEMA_TYPE.getName(), childParameter1);

        TaCoKitElementParameter childParameter2 = new TaCoKitElementParameter(getNode());
        childParameter2.setCategory(EComponentCategory.BASIC);
        childParameter2.setContext(EConnectionType.FLOW_MAIN.getName());
        childParameter2.setDisplayName("Repository");
        childParameter2.setFieldType(EParameterFieldType.TECHNICAL);
        childParameter2.setListItemsDisplayName(new String[0]);
        childParameter2.setListItemsValue(new String[0]);
        childParameter2.setName(EParameterName.REPOSITORY_SCHEMA_TYPE.getName());
        childParameter2.setParentParameter(schema);
        childParameter2.setRequired(true);
        childParameter2.setShow(false);
        childParameter2.setValue("");
        schema.getChildParameters().put(EParameterName.REPOSITORY_SCHEMA_TYPE.getName(), childParameter2);

        addSetting(schema);
    }
}
