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
package org.talend.sdk.component.tcketl.domain.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IconDataTest {

    @Test
    void contentIsCopiedOnTheWayInAndOut() {
        final byte[] bytes = { 1, 2, 3 };
        final IconData icon = new IconData(bytes, "image/png");

        bytes[0] = 9;
        icon.content()[1] = 9;

        assertArrayEquals(new byte[] { 1, 2, 3 }, icon.content());
    }

    @Test
    void equalityComparesTheBytes() {
        final IconData icon = new IconData(new byte[] { 1 }, "image/png");

        assertEquals(icon, new IconData(new byte[] { 1 }, "image/png"));
        assertEquals(icon.hashCode(), new IconData(new byte[] { 1 }, "image/png").hashCode());
        assertNotEquals(icon, new IconData(new byte[] { 2 }, "image/png"));
        assertNotEquals(icon, new IconData(new byte[] { 1 }, "image/svg+xml"));
    }

    @Test
    void svgIsRecognisedFromTheMediaType() {
        assertTrue(new IconData(new byte[0], "image/svg+xml").isSvg());
        assertFalse(new IconData(new byte[0], "image/png").isSvg());
        assertFalse(new IconData(new byte[0], null).isSvg());
    }
}
