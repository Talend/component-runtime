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
package org.talend.sdk.component.runtime.manager.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OverridesTest {

    @Test
    void override() {
        try {
            System.setProperty("testoveride.value1", "overide");
            final Map<String, String> firstMap = new HashMap<>();
            firstMap.put("value1", "v1");
            firstMap.put("value2", "v2");

            final Map<String, String> override = Overrides.override("testoveride", firstMap);

            Assertions.assertEquals("overide", override.get("value1"));
            Assertions.assertEquals("v2", override.get("value2"));
            Assertions.assertEquals("v1", firstMap.get("value1"));
        } finally {
            System.clearProperty("testoveride.value1");
        }

    }
}