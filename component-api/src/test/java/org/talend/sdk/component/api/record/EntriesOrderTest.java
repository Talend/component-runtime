/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.record;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EntriesOrderTest {

    @Test
    void testMoveAfter() {
        final Schema.EntriesOrder order = new Schema.EntriesOrder("f1,f2,f3,f4");
        Assertions.assertEquals("f1,f2,f3,f4", order.toFields());

        order.moveAfter("f4", "f3");
        Assertions.assertEquals("f1,f2,f4,f3", order.toFields());

        order.moveAfter("f1", "f2");
        Assertions.assertEquals("f1,f2,f4,f3", order.toFields());

        order.moveAfter("f1", "f4");
        Assertions.assertEquals("f1,f4,f2,f3", order.toFields());

        order.moveAfter("f4", "f1");
        Assertions.assertEquals("f4,f1,f2,f3", order.toFields());
    }

    @Test
    void testMoveBefore() {
        final Schema.EntriesOrder order = new Schema.EntriesOrder("f1,f2,f3,f4");
        Assertions.assertEquals("f1,f2,f3,f4", order.toFields());

        order.moveBefore("f4", "f3");
        Assertions.assertEquals("f1,f2,f3,f4", order.toFields());

        order.moveBefore("f1", "f2");
        Assertions.assertEquals("f2,f1,f3,f4", order.toFields());

        order.moveBefore("f1", "f4");
        Assertions.assertEquals("f2,f4,f1,f3", order.toFields());

        order.moveBefore("f4", "f1");
        Assertions.assertEquals("f2,f1,f4,f3", order.toFields());
    }

}