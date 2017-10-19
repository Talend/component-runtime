// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.studio;

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
