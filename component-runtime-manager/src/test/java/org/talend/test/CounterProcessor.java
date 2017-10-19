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
package org.talend.test;

import java.io.Serializable;

import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Output;
import org.talend.component.api.processor.OutputEmitter;
import org.talend.component.api.processor.Processor;

@Processor(family = "chain", name = "count")
public class CounterProcessor implements Serializable {

    private int previous = 0;

    @ElementListener
    public void length(final String data, @Output final OutputEmitter<Integer> main,
            @Output("rejected") final OutputEmitter<String> rejects) {
        if (data.contains("reject")) {
            rejects.emit(data);
        } else {
            main.emit(previous += data.length());
        }
    }
}
