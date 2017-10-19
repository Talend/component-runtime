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
package org.talend.components.runtime.input;

import java.util.List;

import org.talend.components.runtime.base.Lifecycle;

public interface Mapper extends Lifecycle {

    long assess();

    List<Mapper> split(final long desiredSize);

    Input create();

    boolean isStream();
}
