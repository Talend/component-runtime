/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.junit.Test;
import org.talend.sdk.component.api.configuration.action.Discoverable;
import org.talend.sdk.component.api.configuration.action.Proposable;

public class ActionParameterEnricherTest {

    @Test
    public void condition() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::action::dynamic_values", "test");
            }
        }, new ActionParameterEnricher().onParameterAnnotation("testParam", String.class, new Proposable() {

            @Override
            public String value() {
                return "test";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Proposable.class;
            }
        }));
    }

    @Test
    public void discoverable() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::action::schema::binding", "ALL");
                put("tcomp::action::schema", "test");
            }
        }, new ActionParameterEnricher().onParameterAnnotation("testParam", String.class, new Discoverable() {

            @Override
            public String value() {
                return "test";
            }

            @Override
            public Binding binding() {
                return Binding.ALL;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Discoverable.class;
            }
        }));
    }
}
