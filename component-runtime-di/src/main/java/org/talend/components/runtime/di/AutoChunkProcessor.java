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
package org.talend.components.runtime.di;

import org.talend.components.runtime.base.Lifecycle;
import org.talend.components.runtime.output.InputFactory;
import org.talend.components.runtime.output.OutputFactory;
import org.talend.components.runtime.output.Processor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AutoChunkProcessor implements Lifecycle {

    private static final OutputFactory FAILING_OUTPUT_FACTORY = name -> {
        throw new IllegalArgumentException("Output from @AfterGroup is not supported here");
    };

    private final int chunkSize;

    private final Processor processor;

    private int processedItemCount = 0;

    public void onElement(final InputFactory ins, final OutputFactory outs) {
        if (processedItemCount == 0) {
            processor.beforeGroup();
        }
        try {
            processor.onNext(ins, outs);
            processedItemCount++;
        } finally {
            if (processedItemCount == chunkSize) {
                processor.afterGroup(outs);
            }
        }
    }

    @Override
    public void stop() {
        try {
            if (processedItemCount > 0) {
                processor.afterGroup(FAILING_OUTPUT_FACTORY);
            }
        } finally {
            processor.stop();
        }
    }

    @Override
    public String plugin() {
        return processor.plugin();
    }

    @Override
    public String rootName() {
        return processor.rootName();
    }

    @Override
    public String name() {
        return processor.name();
    }

    @Override
    public void start() {
        processor.start();
    }
}
