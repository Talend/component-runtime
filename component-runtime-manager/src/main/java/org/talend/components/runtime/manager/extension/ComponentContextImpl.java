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
package org.talend.components.runtime.manager.extension;

import static lombok.AccessLevel.NONE;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.talend.components.spi.component.ComponentExtension;

@Data
public class ComponentContextImpl implements ComponentExtension.ComponentContext {
    private final Class<?> type;
    private boolean noValidation;

    @Setter(NONE)
    private ComponentExtension owningExtension;

    @Getter(NONE)
    private ComponentExtension currentExtension;

    @Override
    public void skipValidation() {
        if (!noValidation) {
            owningExtension = currentExtension;
            noValidation = true;
        }
    }
}
