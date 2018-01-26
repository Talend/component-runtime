/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.ui.guessschema;

import java.util.ArrayList;
import java.util.List;

import org.talend.core.model.components.IComponent;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.Property;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.process.DataProcess;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.properties.controllers.AbstractGuessSchemaProcess;
import org.talend.sdk.component.studio.Lookups;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.metadata.model.ComponentModelSpy;
import org.talend.sdk.component.studio.util.TaCoKitConst;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitGuessSchemaProcess extends AbstractGuessSchemaProcess {

    public TaCoKitGuessSchemaProcess(final Property property, final INode node, final IContext selectContext) {
        super(property, node, selectContext);
    }

    @Override
    protected void buildProcess() {
        Process process = new Process(getProperty());
        setProcess(process);
        INode node = getNode();
        configContext(process, node);

        List<? extends IConnection> outgoingConnections = new ArrayList<>(node.getOutgoingConnections());
        try {
            node.setOutgoingConnections(new ArrayList<>());
            DataProcess dataProcess = new DataProcess(process);
            INode newNode = dataProcess.buildNodeFromNode(node, process);

            IComponent component = newNode.getComponent();
            ComponentModelSpy componentSpy = createComponnetModelSpy(component);
            newNode.setComponent(componentSpy);
            setNode(newNode);

            IElementParameter tempFileElemParam = new ElementParameter(newNode);
            tempFileElemParam.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_TEMP_FILE_KEY);
            tempFileElemParam.setValue(getTemppath().toPortableString());
            IElementParameter encodingElemParam = new ElementParameter(newNode);
            encodingElemParam.setName(TaCoKitConst.GUESS_SCHEMA_PARAMETER_ENCODING_KEY);
            encodingElemParam.setValue(getCurrentProcessEncoding());

            List<IElementParameter> elementParameters = (List<IElementParameter>) newNode.getElementParameters();
            elementParameters.add(tempFileElemParam);
            elementParameters.add(encodingElemParam);
        } finally {
            node.setOutgoingConnections(outgoingConnections);
        }
    }

    @Override
    protected boolean isCheckError() {
        return true;
    }

    @Override
    protected String getErrorMessage() {
        return Messages.getString("guessSchema.error"); //$NON-NLS-1$
    }

    private ComponentModelSpy createComponnetModelSpy(final IComponent component) {
        ComponentModelSpy componentSpy = new ComponentModelSpy(component);
        IComponent guessComponent = Lookups.taCoKitCache().getTaCoKitGuessSchemaComponent();
        componentSpy.spyName(guessComponent.getName());
        componentSpy.spyOriginalName(guessComponent.getOriginalName());
        componentSpy.spyShortName(guessComponent.getShortName());
        componentSpy.spyTemplateFolder(guessComponent.getTemplateFolder());
        componentSpy.spyTemplateNamePrefix(guessComponent.getTemplateNamePrefix());
        componentSpy.spyAvailableCodeParts(guessComponent.getAvailableCodeParts());
        return componentSpy;
    }

}
