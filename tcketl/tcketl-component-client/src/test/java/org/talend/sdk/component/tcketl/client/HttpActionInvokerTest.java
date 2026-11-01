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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome.Choice;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

/** {@code POST /action/execute}: the request that is sent and the decoding of each type of answer. */
class HttpActionInvokerTest {

    private static ActionOutcome run(final StubServer stub, final String type, final String answer)
            throws IOException {
        stub.on("/action/execute", StubServer.Reply.json(200, answer));
        return ComponentServerClient
                .create(stub.endpoint(), ServerTimeouts.defaults(), "fr")
                .actions()
                .execute(new ActionRequest("fam", type, "act", Map.of("datastore.url", "jdbc:x")));
    }

    @Test
    void theRequestCarriesTheTripleTheLanguageAndTheFlatBody() throws IOException {
        try (StubServer stub = StubServer.start()) {
            run(stub, "healthcheck", "{\"status\":\"OK\"}");

            final StubServer.Request sent = stub.requests().get(0);
            assertEquals("POST", sent.method());
            assertEquals(List.of("fam"), sent.parameters("family"));
            assertEquals(List.of("healthcheck"), sent.parameters("type"));
            assertEquals(List.of("act"), sent.parameters("action"));
            assertEquals(List.of("fr"), sent.parameters("lang"));
            assertEquals("{\"datastore.url\":\"jdbc:x\"}", sent.body());
        }
    }

    @Test
    void aStatusIsDecodedForHealthchecksAndValidations() throws IOException {
        try (StubServer stub = StubServer.start()) {
            assertEquals(new ActionOutcome.Checked(true, ""), run(stub, "healthcheck", "{\"status\":\"OK\"}"));
            assertEquals(new ActionOutcome.Checked(false, "refused"),
                    run(stub, "validation", "{\"status\":\"KO\",\"comment\":\"refused\"}"));
        }
    }

    @Test
    void choicesKeepTheIdAndDropEntriesWithoutOne() throws IOException {
        try (StubServer stub = StubServer.start()) {
            assertEquals(new ActionOutcome.Choices(true, List.of(new Choice("a", "A"), new Choice("b", "b"))),
                    run(stub, "suggestions",
                            "{\"cacheable\":true,\"items\":[{\"id\":\"a\",\"label\":\"A\"},{\"label\":\"x\"},{\"id\":\"b\"}]}"));
        }
    }

    @Test
    void anUpdateIsFlattenedAndASchemaGivesItsColumns() throws IOException {
        try (StubServer stub = StubServer.start()) {
            assertEquals(
                    new ActionOutcome.Replacement(
                            Map.of(".url", "u", ".list[0]", "x", ".list[1]", "y", ".n.deep", "3")),
                    run(stub, "update", "{\"url\":\"u\",\"list\":[\"x\",\"y\"],\"n\":{\"deep\":3},\"gone\":null}"));
            assertEquals(new ActionOutcome.Columns(List.of("id", "name")),
                    run(stub, "schema", "{\"entries\":[{\"name\":\"id\"},{\"name\":\"name\"}],\"type\":\"RECORD\"}"));
        }
    }

    @Test
    void aServerErrorIsRaisedWithItsPayload() throws IOException {
        try (StubServer stub = StubServer.start()) {
            stub.on("/action/execute",
                    StubServer.Reply.json(520, "{\"code\":\"ACTION_ERROR\",\"description\":\"it failed\"}"));
            final ServerException error = assertThrows(ServerException.class,
                    () -> ComponentServerClient
                            .create(stub.endpoint(), ServerTimeouts.defaults(), "en")
                            .actions()
                            .execute(new ActionRequest("fam", "healthcheck", "act", Map.of())));
            assertEquals(520, error.status());
            assertEquals("ACTION_ERROR", error.code());
        }
    }
}
