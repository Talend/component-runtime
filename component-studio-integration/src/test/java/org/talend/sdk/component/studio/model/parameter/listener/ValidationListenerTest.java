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
package org.talend.sdk.component.studio.model.parameter.listener;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.studio.model.action.ActionParameter;
import org.talend.sdk.component.studio.model.parameter.ValidationLabel;

class ValidationListenerTest {

    @Test
    void simple() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        final ValidationLabel label = new ValidationLabel(null);
        final ValidationListener listener = new ValidationListener(label, "test", "validation") {

            @Override
            public Map<String, String> callback() {
                assertTrue(executed.compareAndSet(false, true));

                final String param = parameters.payload().get("the.test.param");
                assertTrue(param != null && param.endsWith("foo"));
                return singletonMap("status", "OK");
            }
        };
        listener.addParameter(new ActionParameter("test", "the.test.param", null));

        // normal
        listener.propertyChange(new PropertyChangeEvent(new Object(), "test", null, "foo"));
        assertTrue(executed.get());

        // quoted string
        executed.set(false);
        listener.propertyChange(new PropertyChangeEvent(new Object(), "test", null, "\"foo\""));
        assertTrue(executed.get());

        // context value -> skip the execution
        executed.set(false);
        listener.propertyChange(new PropertyChangeEvent(new Object(), "test", null, "context.foo"));
        assertFalse(executed.get());
    }
}
