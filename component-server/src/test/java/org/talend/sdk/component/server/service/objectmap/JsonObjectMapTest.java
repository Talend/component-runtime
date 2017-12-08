/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service.objectmap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import javax.json.Json;

import org.junit.Test;
import org.talend.sdk.component.api.processor.data.ObjectMap;

public class JsonObjectMapTest {

    private final ObjectMap map = new JsonObjectMap(Json
            .createObjectBuilder()
            .add("directString", "test")
            .add("directInt", 1)
            .add("nested",
                    Json
                            .createObjectBuilder()
                            .add("value", "n")
                            .add("list", Json.createArrayBuilder().add(1).add(2).build())
                            .build())
            .build());

    @Test
    public void get() {
        assertEquals("test", map.get("directString"));
        assertEquals(1L, map.get("directInt"));
        assertEquals("n", map.get("nested.value"));
        assertEquals(asList(1L, 2L), map.get("nested.list"));
    }
}
