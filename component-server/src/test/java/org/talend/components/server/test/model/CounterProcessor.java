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
package org.talend.components.server.test.model;

import java.io.Serializable;

import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

@Processor(family = "chain", name = "count")
public class CounterProcessor implements Serializable {

    private int previous = 0;

    @ElementListener
    public int length(final String data) {
        return previous += data.length();
    }
}
