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
package org.talend.components.runtime.internationalization;

import java.util.Optional;
import java.util.ResourceBundle;

public class ParameterBundle extends InternalBundle {

    public ParameterBundle(final ResourceBundle bundle, final String prefix) {
        super(bundle, prefix);
    }

    public Optional<String> displayName() {
        return readValue("_displayName");
    }
}
