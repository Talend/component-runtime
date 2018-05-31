/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.service;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.proxy.service.client.UiSpecServiceClient;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;

@CdiInject
@WithServer
class UiSpecServiceClientTest {

    @Inject
    private UiSpecServiceProvider factory;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Test
    void findRoots() throws Exception {
        try (final UiSpecServiceClient client = factory.newClient("en", k -> null)) {
            final Map<String, Object> result =
                    client.action("whatever", "dynamic_values", "builtin::roots", emptyMap()).get();
            assertEquals(1, result.size());
            final List<Map<String, String>> items = List.class.cast(result.get("items"));
            assertEquals(3, items.size());
            final Map<String, String> itemsMap = items.stream().collect(toMap(e -> e.get("id"), e -> e.get("label")));
            assertEquals(new HashMap<String, String>() {

                {
                    put("VGhlVGVzdEZhbWlseTIjZGF0YXN0b3JlI0Nvbm5lY3Rpb24tMQ", "Connection-1");
                    put("VGhlVGVzdEZhbWlseSNkYXRhc3RvcmUjQ29ubmVjdGlvbi0y", "Connection-2");
                    put("VGhlVGVzdEZhbWlseSNkYXRhc3RvcmUjQ29ubmVjdGlvbi0z", "Connection-3");
                }
            }, itemsMap);
        }
    }

    @Test
    void jsonPatchNewForm() throws Exception {
        try (final UiSpecServiceClient client = factory.newClient("en", k -> null)) {
            final Map<String, Object> wrapper = client
                    .action("whatever", "jsonpatch", "builtin::root::reloadFromId", new HashMap<String, Object>() {

                        {
                            put("id", "VGhlVGVzdEZhbWlseTIjZGF0YXN0b3JlI0Nvbm5lY3Rpb24tMQ");
                        }
                    })
                    .get();
            assertEquals(2, wrapper.size());
            final Collection<Map<String, Object>> results = Collection.class.cast(wrapper.get("jsonPatch"));
            assertEquals(1, results.size());
            final Map<String, Object> meta = Map.class.cast(wrapper.get("metadata"));
            assertEquals("myicon", meta.get("icon"));
            final Map<String, Object> result = results.iterator().next();
            assertEquals("replace", result.get("op"));
            assertEquals("/", result.get("path"));
            final Ui ui = jsonb.fromJson(jsonb.toJson(result.get("value")), Ui.class);
            assertNull(ui.getProperties());
            assertNotNull(ui.getJsonSchema());
            assertNotNull(ui.getUiSchema());
            // just some sanity checks, we assume the serialization works here and form-core already tested that part
            assertEquals(1, ui.getUiSchema().size());
            assertEquals(3, ui.getUiSchema().iterator().next().getItems().size());
        }
    }
}
