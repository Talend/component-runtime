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
package org.talend.components.sample;

import static java.util.Collections.emptyMap;

import java.util.HashMap;

import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.chain.CountingSuccessListener;
import org.talend.components.runtime.manager.chain.ExecutionChainBuilder;
import org.talend.components.runtime.manager.chain.ToleratingErrorHandler;

public class Main {

    public static void main(final String[] args) {
        // tag::main[]
        try (final ComponentManager manager = ComponentManager.instance()) {
            ExecutionChainBuilder.start().withConfiguration("SampleJob", true)
                    .fromInput("sample", "reader", 2, new HashMap<String, String>() {

                        {
                            put("file", "/tmp/input.csv");
                        }
                    }).toProcessor("sample", "mapper", 1, emptyMap())
                    .toProcessor(null, "sample", "writer", 1, new HashMap<String, String>() {

                        {
                            put("file", "/tmp/output.csv");
                        }
                    }).create(manager, plugin -> null, new CountingSuccessListener(), new ToleratingErrorHandler(0)).get()
                    .execute();
        }
        // end::main[]
    }
}
