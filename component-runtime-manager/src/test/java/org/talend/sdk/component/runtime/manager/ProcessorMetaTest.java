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
package org.talend.sdk.component.runtime.manager;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.ProcessorMeta;

import lombok.Data;

/**
 * Unit-tests for {@link ProcessorMeta}
 */
public class ProcessorMetaTest {

    @Test
    public void testGetListener() {
        ComponentFamilyMeta parent = new ComponentFamilyMeta("plugin", Collections.emptyList(), "default", "name");
        ProcessorMeta meta = new ProcessorMeta(parent, "name", "default", 1, TestProcessor.class, null, null, null, true);
        Method listener = meta.getListener();
        Assert.assertEquals("map", listener.getName());
        Assert.assertEquals(4, listener.getParameterCount());
    }

    @Test
    public void testGetInputFlows() {
        ComponentFamilyMeta parent = new ComponentFamilyMeta("plugin", Collections.emptyList(), "default", "name");
        ProcessorMeta meta = new ProcessorMeta(parent, "name", "default", 1, TestProcessor.class, null, null, null, true);
        Collection<String> inputs = meta.getInputFlows();
        Assert.assertEquals(2, inputs.size());
        Assert.assertTrue(inputs.contains("__default__"));
        Assert.assertTrue(inputs.contains("REJECT"));
    }

    @Test
    public void testGetOutputFlows() {
        ComponentFamilyMeta parent = new ComponentFamilyMeta("plugin", Collections.emptyList(), "default", "name");
        ProcessorMeta meta = new ProcessorMeta(parent, "name", "default", 1, TestProcessor.class, null, null, null, true);
        Collection<String> outputs = meta.getOutputFlows();
        Assert.assertEquals(2, outputs.size());
        Assert.assertTrue(outputs.contains("__default__"));
        Assert.assertTrue(outputs.contains("OUTPUT"));
    }

    @Processor
    private static class TestProcessor {

        @ElementListener
        public void map(@Input final InputData1 input1, @Input("REJECT") final InputData2 input2,
                @Output final OutputEmitter<OutputData1> output1, @Output("OUTPUT") final OutputEmitter<OutputData2> output2) {
            // no-op
        }

    }

    @Data
    private static class InputData1 {

        private String value;
    }

    @Data
    private static class InputData2 {

        private int value;
    }

    @Data
    private static class OutputData1 {

        private String name;

        private int age;
    }

    @Data
    private static class OutputData2 {

        private String error;

        private int id;
    }
}
