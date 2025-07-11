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
package org.talend.sdk.component.runtime.manager.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LazyMapTest {

    int multiplyTwoSollicitation = 0;

    @Test
    void get() {
        LazyMap<Integer, Integer> map = new LazyMap<>(30, this::multiplyTwo);

        Assertions.assertEquals(2, map.get(1));
        Assertions.assertEquals(1, this.multiplyTwoSollicitation);

        Assertions.assertEquals(2, map.get(1));
        Assertions.assertEquals(1, this.multiplyTwoSollicitation);

        Assertions.assertEquals(8, map.get(4));
        Assertions.assertEquals(2, this.multiplyTwoSollicitation);

        Assertions.assertNull(map.get(0));
        Assertions.assertEquals(3, this.multiplyTwoSollicitation);
        Assertions.assertNull(map.get(0));
        Assertions.assertEquals(4, this.multiplyTwoSollicitation);

    }

    private Integer multiplyTwo(Integer b) {
        multiplyTwoSollicitation++;
        if (b == 0) {
            return null; // test if null.
        }
        return b * 2;
    }
}