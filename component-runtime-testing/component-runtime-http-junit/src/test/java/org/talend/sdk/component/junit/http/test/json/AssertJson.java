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
package org.talend.sdk.component.junit.http.test.json;

import static lombok.AccessLevel.PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class AssertJson {

    public static void assertJSONEquals(final String expected, final String actual) {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals(read(expected, jsonb), read(actual, jsonb));
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private static JsonValue read(final String json, final Jsonb jsonb) {
        return jsonb.fromJson(json, JsonValue.class);
    }
}
