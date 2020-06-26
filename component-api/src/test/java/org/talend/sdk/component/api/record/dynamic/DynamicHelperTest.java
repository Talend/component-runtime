/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.record.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.talend.sdk.component.api.record.dynamic.DynamicHelper.DYNAMIC_MARKER;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class DynamicHelperTest {

    @Test
    void isDynamicColumn() {
        assertFalse(DynamicHelper.isDynamicColumn(null));
        assertFalse(DynamicHelper.isDynamicColumn(""));
        assertFalse(DynamicHelper.isDynamicColumn("original"));
        assertFalse(DynamicHelper.isDynamicColumn("dynamic" + DYNAMIC_MARKER + "$$"));
        assertTrue(DynamicHelper.isDynamicColumn("dynamic" + DYNAMIC_MARKER));
        assertTrue(DynamicHelper.isDynamicColumn("dynamic$$$" + DYNAMIC_MARKER));
    }

    @Test
    void hasDynamicColumn() {
        assertTrue(DynamicHelper.hasDynamicColumn(Arrays.asList("", null, "original", "dynamic" + DYNAMIC_MARKER)));
        assertFalse(DynamicHelper.hasDynamicColumn(Arrays.asList("one", "two", "three", null, "five")));
    }

    @Test
    void getRealColumnName() {
        assertNull(DynamicHelper.getRealColumnName(null));
        assertEquals("", DynamicHelper.getRealColumnName(""));
        assertEquals("original", DynamicHelper.getRealColumnName("original"));
        assertEquals("dynamic", DynamicHelper.getRealColumnName("dynamic" + DYNAMIC_MARKER));
        assertEquals("dynamic$$", DynamicHelper.getRealColumnName("dynamic$$" + DYNAMIC_MARKER));
    }

    @Test
    void getDynamicRealColumnName() {
        assertEquals("dynamic", DynamicHelper
                .getDynamicRealColumnName(Arrays.asList("", null, "original", "dynamic" + DYNAMIC_MARKER)));
        assertNull(DynamicHelper.getDynamicRealColumnName(Arrays.asList("one", "two", "three", null, "five")));
    }

}