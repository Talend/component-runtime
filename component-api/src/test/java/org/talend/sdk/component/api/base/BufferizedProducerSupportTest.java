/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.base;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class BufferizedProducerSupportTest {

    @Test
    void iterate() {
        final Iterator<List<String>> strings = asList(asList("a", "b"), asList("c"), asList("d", "e", "f")).iterator();
        final BufferizedProducerSupport<String> support =
                new BufferizedProducerSupport<>(() -> strings.hasNext() ? strings.next().iterator() : null);
        assertEquals("a", support.next());
        assertEquals("b", support.next());
        assertEquals("c", support.next());
        assertEquals("d", support.next());
        assertEquals("e", support.next());
        assertEquals("f", support.next());
        assertNull(support.next());
    }
}
