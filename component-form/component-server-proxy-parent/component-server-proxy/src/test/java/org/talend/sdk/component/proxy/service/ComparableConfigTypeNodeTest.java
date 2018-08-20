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

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.openejb.loader.IO;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

class ComparableConfigTypeNodeTest {

    @Test
    void min() throws Exception {
        final ConfigTypeNodes nodes;
        try (final Jsonb jsonb = JsonbBuilder.create();
                final InputStream stream =
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("nodes.json")) {
            nodes = jsonb.fromJson(IO.slurp(stream), ConfigTypeNodes.class);
        }

        assertEquals("RmlsZUlPI2RhdGFzdG9yZSNTM0RhdGFTZXQ",
                min(nodes, "RmlsZUlPI2RhdGFzdG9yZSNTM0RhdGFTdG9yZQ").getId());
        assertEquals("RmlsZUlPI2RhdGFzdG9yZSNTaW1wbGVGaWxlSU9EYXRhU2V0",
                min(nodes, "RmlsZUlPI2RhdGFzdG9yZSNTaW1wbGVGaWxlSU9EYXRhU3RvcmU").getId());
    }

    private ConfigTypeNode min(final ConfigTypeNodes nodes, final String parentId) {
        return nodes
                .getNodes()
                .values()
                .stream()
                .filter(node -> node.getParentId() != null && node.getParentId().equals(parentId))
                .min(comparing(ActionService.ComparableConfigTypeNode::new))
                .orElseThrow(() -> new IllegalStateException("No child form"));
    }
}
