/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.domain.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LinkTest {

    @Test
    void factoryBuildsBothEndpoints() {
        final Link link = Link.of(new NodeId("n-1"), "__default__", new NodeId("n-2"), "lookup");

        assertEquals(new Endpoint(new NodeId("n-1"), "__default__"), link.from());
        assertEquals(new Endpoint(new NodeId("n-2"), "lookup"), link.to());
        assertEquals("n-1.__default__ -> n-2.lookup", link.toString());
    }

    @Test
    void endpointsNeedANodeAndAFlow() {
        assertThrows(NullPointerException.class, () -> new Endpoint(null, "__default__"));
        assertThrows(IllegalArgumentException.class, () -> new Endpoint(new NodeId("n-1"), " "));
        assertThrows(IllegalArgumentException.class, () -> new NodeId(""));
    }
}
