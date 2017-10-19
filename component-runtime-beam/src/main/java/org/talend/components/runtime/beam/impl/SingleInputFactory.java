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
package org.talend.components.runtime.beam.impl;

import lombok.Data;

import org.talend.components.runtime.output.InputFactory;

@Data
class SingleInputFactory implements InputFactory {

    private final Object out;

    @Override
    public Object read(final String name) {
        return out;
    }
}
