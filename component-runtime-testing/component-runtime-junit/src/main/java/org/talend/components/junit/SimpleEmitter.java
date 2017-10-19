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
package org.talend.components.junit;

import java.io.Serializable;

import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.Producer;

@Emitter(family = "test", name = "emitter")
public class SimpleEmitter implements Serializable {

    @Producer
    public Object next() {
        final SimpleComponentRule.State state = SimpleComponentRule.STATE.get();
        return state.emitter.next();
    }
}
