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
package org.talend.sdk.component.tcketl.domain.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.tcketl.domain.form.Props.map;
import static org.talend.sdk.component.tcketl.domain.form.Props.object;
import static org.talend.sdk.component.tcketl.domain.form.Props.string;
import static org.talend.sdk.component.tcketl.domain.form.Props.tree;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionBinding;
import org.talend.sdk.component.tcketl.domain.property.ActionDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

/** The request body of a service: the form values renamed after the parameters of the action. */
class ActionBodiesTest {

    private static final PropertyTree TREE = tree(object("cfg"), object("cfg.conn"), string("cfg.conn.url"),
            string("cfg.conn.user"), string("cfg.conn.empty"), string("cfg.conn.pwd"), string("cfg.driver"));

    private static final List<ActionDefinition> ACTIONS = List.of(
            new ActionDefinition("fam", "healthcheck", "check", "Check",
                    List.of(PropertyDefinition.builder("datastore", PropertyType.OBJECT).parameterIndex(0).build())),
            new ActionDefinition("fam", "validation", "driver", "Driver",
                    List.of(PropertyDefinition.builder("value", PropertyType.STRING).parameterIndex(0).build())));

    private static final FormValues VALUES = FormValues.of(TREE,
            map("cfg.conn.url", "jdbc:x", "cfg.conn.user", "me", "cfg.driver", "d1"));

    @Test
    void anObjectIsSentUnderTheNameOfTheParameterWithoutItsOwnPath() {
        final ActionRequest request = ActionBodies
                .request("fam", new ActionBinding("healthcheck", "check", List.of("."), Map.of()), ACTIONS,
                        "cfg.conn", VALUES)
                .orElseThrow();

        assertEquals(Map.of("datastore.url", "jdbc:x", "datastore.user", "me"), request.body());
    }

    @Test
    void aPrimitiveIsSentUnderTheNameOfTheParameterAndARelativeReferenceIsResolved() {
        final ActionRequest request = ActionBodies
                .request("fam", new ActionBinding("validation", "driver", List.of("."), Map.of()), ACTIONS,
                        "cfg.driver", VALUES)
                .orElseThrow();

        assertEquals(Map.of("value", "d1"), request.body());
        assertEquals(Map.of("datastore.url", "jdbc:x", "datastore.user", "me"),
                ActionBodies
                        .request("fam", new ActionBinding("healthcheck", "check", List.of("conn"), Map.of()), ACTIONS,
                                "cfg.driver", VALUES)
                        .orElseThrow()
                        .body());
    }

    @Test
    void anActionTheConnectorDoesNotDescribeCannotBeCalled() {
        assertTrue(ActionBodies
                .request("fam", new ActionBinding("healthcheck", "other"), ACTIONS, "cfg.conn", VALUES)
                .isEmpty());
    }
}
