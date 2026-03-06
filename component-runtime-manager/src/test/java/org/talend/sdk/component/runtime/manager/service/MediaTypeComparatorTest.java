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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MediaTypeComparatorTest {

    @Test
    void codecComparatorTest() {
        final List<String> keys = new ArrayList<>(asList("*/*", "application/json", "application/*+json", "*/json",
                "application/talend+json", "application/atom+json"));
        keys.sort(new MediaTypeComparator(new ArrayList<>(keys)));
        assertEquals(asList("application/json", "application/talend+json", "application/atom+json",
                "application/*+json", "*/json", "*/*"), keys);
    }

}
