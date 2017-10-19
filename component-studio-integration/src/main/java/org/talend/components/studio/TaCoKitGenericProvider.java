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
import java.util.Set;

import org.talend.core.model.components.IComponent;
import org.talend.core.model.components.IComponentsFactory;
import org.talend.core.model.process.IGenericProvider;
import org.talend.core.ui.component.ComponentsFactoryProvider;
import org.talend.repository.ProjectManager;

public class TaCoKitGenericProvider implements IGenericProvider {

    @Override
    public void loadComponentsFromExtensionPoint() {
        if (ProjectManager.getInstance().getCurrentProject() == null) {
            return;
        }

        final IComponentsFactory factory = ComponentsFactoryProvider.getInstance();
        final Set<IComponent> components = factory.getComponents();
        synchronized (components) {
            components.removeIf(ComponentModel.class::isInstance);
            components.add(new ComponentModel());
        }
    }

    @Override // unused
    public List<?> addPaletteEntry() {
        return emptyList();
    }
}
