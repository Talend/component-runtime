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
package org.talend.components.runtime.base;

import java.io.Serializable;

public abstract class Named implements Lifecycle, Serializable {

    private String name;

    private String rootName;

    private String plugin;

    protected Named(final String rootName, final String name, final String plugin) {
        this.rootName = rootName;
        this.name = name;
        this.plugin = plugin;
    }

    protected Named() {
        // no-op
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return rootName;
    }

    @Override
    public String name() {
        return name;
    }
}
