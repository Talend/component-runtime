/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.component.studio;

import static java.util.Collections.emptyList;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.model.components.EComponentType;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.core.model.process.INodeReturn;
import org.talend.core.model.temp.ECodePart;
import org.talend.designer.core.model.components.AbstractBasicComponent;

public class ComponentModel extends AbstractBasicComponent {

    private static final ImageDescriptor DEFAULT_IMAGE = ImageProvider.getImageDesc(EImage.COMPONENT_MISSING);

    @Override
    public String getName() {
        return "testname";
    }

    @Override
    public String getOriginalName() {
        return "test2";
    }

    @Override
    public String getLongName() {
        return "testlong";
    }

    @Override
    public String getOriginalFamilyName() {
        return "originalfamilyname";
    }

    @Override
    public String getTranslatedFamilyName() {
        return "getTranslatedFamilyName";
    }

    @Override
    public String getShortName() {
        return "testshort";
    }

    @Override
    public ImageDescriptor getIcon32() {
        return DEFAULT_IMAGE;
    }

    @Override
    public ImageDescriptor getIcon24() {
        return DEFAULT_IMAGE;
    }

    @Override
    public ImageDescriptor getIcon16() {
        return DEFAULT_IMAGE;
    }

    @Override
    public List<? extends IElementParameter> createElementParameters(final INode iNode) {
        return emptyList();
    }

    @Override
    public List<? extends INodeReturn> createReturns(final INode iNode) {
        return emptyList();
    }

    @Override
    public List<? extends INodeConnector> createConnectors(final INode iNode) {
        return emptyList();
    }

    @Override
    public boolean isSchemaAutoPropagated() {
        return false;
    }

    @Override
    public boolean isDataAutoPropagated() {
        return false;
    }

    @Override
    public List<ModuleNeeded> getModulesNeeded() {
        return emptyList();
    }

    @Override
    public List<ModuleNeeded> getModulesNeeded(final INode iNode) {
        return emptyList();
    }

    @Override
    public List<ECodePart> getAvailableCodeParts() {
        return emptyList();
    }

    @Override
    public EComponentType getComponentType() {
        return EComponentType.GENERIC;
    }
}
