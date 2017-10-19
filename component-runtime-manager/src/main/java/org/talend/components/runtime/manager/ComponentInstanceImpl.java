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
package org.talend.components.runtime.manager;

import org.talend.components.spi.component.ComponentExtension;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ComponentInstanceImpl implements ComponentExtension.ComponentInstance {

    private final Object instance;

    private final String plugin;

    private final String family;

    private final String name;

    @Override
    public Object instance() {
        return instance;
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String family() {
        return family;
    }

    @Override
    public String name() {
        return name;
    }
}
