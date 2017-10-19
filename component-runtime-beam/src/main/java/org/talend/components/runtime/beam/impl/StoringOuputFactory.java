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

import java.util.ArrayList;
import java.util.List;

import org.talend.component.api.processor.OutputEmitter;
import org.talend.components.runtime.output.Branches;
import org.talend.components.runtime.output.OutputFactory;

import lombok.Data;
import lombok.Getter;

@Data
class StoringOuputFactory implements OutputFactory {

    @Getter
    private List<Object> values;

    @Override
    public OutputEmitter create(final String name) {
        return value -> {
            if (!Branches.DEFAULT_BRANCH.equals(name)) {
                throw new IllegalArgumentException("Branch " + name + " unsupported for now");
            }
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add(value);
        };
    }
}
