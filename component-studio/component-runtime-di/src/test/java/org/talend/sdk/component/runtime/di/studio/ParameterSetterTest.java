/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.di.studio;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParameterSetterTest {

    protected static RecordBuilderFactory builderFactory;

    // do the same thing with studio
    private static final Map<String, Object> globalMap = Collections.synchronizedMap(new HashMap<>());

    @BeforeAll
    static void forceManagerInit() {
        final ComponentManager manager = ComponentManager.instance();
        if (manager.find(Stream::of).count() == 0) {
            manager.addPlugin(new File("target/test-classes").getAbsolutePath());
        }
    }

    @Test
    void testChangeValue() {
        final ComponentManager manager = ComponentManager.instance();

        final java.util.Map<String, String> configuration = new java.util.HashMap<>();
        configuration.put("configuration.usePreparedStatement", "true");
        configuration.put("configuration.preparedStatementParameters[0].dataValue",
                "new java.math.BigDecimal(\"123\")");
        configuration.put("configuration.preparedStatementParameters[0].index", "1");
        configuration.put("configuration.preparedStatementParameters[0].type", "BigDecimal");
        configuration.put("configuration.preparedStatementParameters[1].dataValue", "abc");
        configuration.put("configuration.preparedStatementParameters[1].index", "2");
        configuration.put("configuration.preparedStatementParameters[1].type", "String");

        final Processor processor = manager.findProcessor("TestFamily", "TestOutput", 1, configuration)
                .orElseThrow(() -> new IllegalStateException("scanning failed"));

        ParameterSetter setter = new ParameterSetter(processor);

        // processor.onNext
        setter.change("configuration.preparedStatementParameters[0].dataValue", new java.math.BigDecimal("456"));
        setter.change("configuration.preparedStatementParameters[1].dataValue", "abc1");
        JDBCRowConfig config =
                TestOutputComponent.class.cast(Delegated.class.cast(processor).getDelegate()).getConfiguration();
        Assert.assertEquals(config.getPreparedStatementParameters().get(0).getDataValue(),
                new java.math.BigDecimal("456"));
        Assert.assertEquals(config.getPreparedStatementParameters().get(1).getDataValue(), "abc1");

        // processor.onNext
        setter.change("configuration.preparedStatementParameters[0].dataValue", new java.math.BigDecimal("789"));
        setter.change("configuration.preparedStatementParameters[1].dataValue", "abc12");
        Assert.assertEquals(config.getPreparedStatementParameters().get(0).getDataValue(),
                new java.math.BigDecimal("789"));
        Assert.assertEquals(config.getPreparedStatementParameters().get(1).getDataValue(), "abc12");
    }

    @org.talend.sdk.component.api.processor.Processor(name = "TestOutput", family = "TestFamily")
    public static class TestOutputComponent implements Serializable {

        @Getter
        private final JDBCRowConfig configuration;

        public TestOutputComponent(@Option("configuration") final JDBCRowConfig configuration) {
            this.configuration = configuration;
        }

        @ElementListener
        public void onElement(final Record record) {
            // do nothing
        }
    }

    @Data
    @GridLayout({
            @GridLayout.Row("usePreparedStatement"),
            @GridLayout.Row("preparedStatementParameters")
    })
    @Documentation("jdbc row")
    public static class JDBCRowConfig implements Serializable {

        @Option
        @Documentation("")
        private boolean usePreparedStatement;

        @Option
        @ActiveIf(target = "usePreparedStatement", value = { "true" })
        @Documentation("")
        private List<PreparedStatementParameter> preparedStatementParameters;

    }

    @Data
    @OptionsOrder({ "index", "type", "dataValue" })
    @NoArgsConstructor
    @AllArgsConstructor
    @Documentation("")
    public static class PreparedStatementParameter implements Serializable {

        @Option
        @Documentation("")
        private String index;

        @Option
        @Documentation("")
        private String type;

        @Option
        @Documentation("")
        private transient Object dataValue;

    }

}
