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
package org.talend.sdk.component.proxy.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.test.WithServer;

// stupid test to ensure the lookup "api" works (= we provided a bean for each types)
@WithServer
class ServicesTest {

    @Test
    void ensureLookupsWork() {
        assertTrue(Stream
                .of(Services.class.getMethods())
                .filter(m -> m.isAnnotationPresent(Services.Binding.class))
                .peek(m -> {
                    try {
                        assertNotNull(m.invoke(null));
                    } catch (final InvocationTargetException | IllegalAccessException e) {
                        fail(e.getMessage());
                    }
                })
                .count() > 0);
    }
}
