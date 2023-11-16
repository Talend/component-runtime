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
package org.talend.sdk.component.server.front.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

class SecurityUtilsTest {

    SecurityUtils secutils = new SecurityUtils();

    @Test
    void hasCipheredKeys() {
        assertTrue(secutils.hasCipheredKeys(new HashMap<String, String>() {

            {
                put("password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
            }
        }));
        assertTrue(secutils.hasCipheredKeys(new HashMap<String, String>() {

            {
                put("configuration.url", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                put("configuration.username", "username0");
                put("configuration.password", "vault:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
            }
        }));
        assertFalse(secutils.hasCipheredKeys(new HashMap<String, String>() {

            {
                put("configuration.url", ":hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                put("configuration.username", "username0");
                put("configuration.password", "vaultv1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
            }
        }));
    }

}