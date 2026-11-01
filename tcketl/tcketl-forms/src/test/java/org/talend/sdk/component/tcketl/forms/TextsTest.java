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
package org.talend.sdk.component.tcketl.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The server leaves {@code ${index}} in the titles of list entries; the form numbers them. */
class TextsTest {

    @ParameterizedTest
    @CsvSource({ "'Tag ${index}',0,Tag 1", "'list[${index}]',2,list[3]", "Plain,4,Plain",
            "'Tag ${index}',-1,'Tag ${index}'" })
    void anEntryTitleIsNumberedFromOne(final String text, final int index, final String expected) {
        assertEquals(expected, Texts.numbered(text, index));
    }
}
