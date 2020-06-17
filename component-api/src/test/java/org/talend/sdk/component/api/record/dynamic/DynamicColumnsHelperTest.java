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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DynamicColumnsHelperTest {

    static String DCM = DynamicColumns.DYNAMIC_COLUMN_MARKER;

    @Test
    void isDynamicColumn() {
        assertFalse(DynamicColumnsHelper.isDynamicColumn(null));
        assertFalse(DynamicColumnsHelper.isDynamicColumn(""));
        assertFalse(DynamicColumnsHelper.isDynamicColumn("original"));
        assertFalse(DynamicColumnsHelper.isDynamicColumn("dynamic" + DCM + "$$"));
        assertTrue(DynamicColumnsHelper.isDynamicColumn("dynamic" + DCM));
        assertTrue(DynamicColumnsHelper.isDynamicColumn("dynamic$$$" + DCM));
    }

    @Test
    void hasDynamicColumn() {
        assertTrue(DynamicColumnsHelper.hasDynamicColumn(Arrays.asList("", null, "original", "dynamic" + DCM)));
        assertFalse(DynamicColumnsHelper.hasDynamicColumn(Arrays.asList("one", "two", "three", null, "five")));
    }

    @Test
    void getRealColumnName() {
        assertNull(DynamicColumnsHelper.getRealColumnName(null));
        assertEquals("", DynamicColumnsHelper.getRealColumnName(""));
        assertEquals("original", DynamicColumnsHelper.getRealColumnName("original"));
        assertEquals("dynamic", DynamicColumnsHelper.getRealColumnName("dynamic" + DCM));
        assertEquals("dynamic$$", DynamicColumnsHelper.getRealColumnName("dynamic" + DCM + "$$"));
    }

    @Test
    void getDynamicRealColumnName() {
        assertEquals("dynamic",
                DynamicColumnsHelper.getDynamicRealColumnName(Arrays.asList("", null, "original", "dynamic" + DCM)));
        assertNull(DynamicColumnsHelper.getDynamicRealColumnName(Arrays.asList("one", "two", "three", null, "five")));
    }

}