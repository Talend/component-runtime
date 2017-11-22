/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.junit;

import org.junit.ClassRule;
import org.junit.Test;
import org.talend.sdk.component.junit.component.Source;
import org.talend.sdk.component.junit.component.Transform;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Processor;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class SimpleComponentRuleTest {

    @ClassRule
    public static final SimpleComponentRule COMPONENT_FACTORY = new SimpleComponentRule(
            "org.talend.sdk.component.junit.component");

    @Test
    public void manualMapper() {
        final Mapper mapper = COMPONENT_FACTORY.createMapper(Source.class, new Source.Config() {{
            values = asList("a", "b");
        }});
        assertFalse(mapper.isStream());
        final Input input = mapper.create();
        assertEquals("a", input.next());
        assertEquals("b", input.next());
        assertNull(input.next());
    }

    @Test
    public void sourceCollector() {
        final Mapper mapper = COMPONENT_FACTORY.createMapper(Source.class, new Source.Config() {{
            values = asList("a", "b");
        }});
        assertEquals(asList("a", "b"), COMPONENT_FACTORY.collectAsList(String.class, mapper));
    }

    @Test
    public void processorCollector() {
        final Processor processor = COMPONENT_FACTORY.createProcessor(Transform.class, null);
        final SimpleComponentRule.Outputs outputs = COMPONENT_FACTORY.collect(processor,
                new JoinInputFactory().withInput("__default__", asList(new Transform.Record("a"), new Transform.Record("bb")))
                                      .withInput("second", asList(new Transform.Record("1"), new Transform.Record("2")))
        );
        assertEquals(2, outputs.size());
        assertEquals(asList(2, 3), outputs.get(Integer.class, "size"));
        assertEquals(asList("a1", "bb2"), outputs.get(String.class, "value"));
    }
}
