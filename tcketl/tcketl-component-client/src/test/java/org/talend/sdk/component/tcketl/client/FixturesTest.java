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
package org.talend.sdk.component.tcketl.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

class FixturesTest {

    private static final List<String> ALL_FIXTURES = List
            .of("sample-environment.json", "sample-component-index.json",
                    "sample-component-details-sample-form-input.json",
                    "sample-component-documentation-cyberark-input.json");

    @Test
    void everyFixtureIsPlainJsonWithoutByteOrderMark() {
        for (final String name : ALL_FIXTURES) {
            assertFalse(Fixtures.readText(name).startsWith("\uFEFF"), () -> name + " must not start with a BOM");
            assertTrue(Fixtures.readJson(name).isObject(), () -> name + " must be a JSON object");
        }
    }

    @Test
    void sampleFormInputDetailsDescribeAnInputConnector() {
        final JsonNode detail =
                Fixtures.readJson("sample-component-details-sample-form-input.json").get("details").get(0);
        assertEquals(0, detail.get("inputFlows").size());
        assertEquals("__default__", detail.get("outputFlows").get(0).asText());
        assertEquals("config", detail.get("properties").get(0).get("path").asText());
        assertEquals("UPDATABLE", detail.get("actions").get(0).get("name").asText());
    }
}
