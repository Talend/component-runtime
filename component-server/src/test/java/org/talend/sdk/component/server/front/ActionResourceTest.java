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
package org.talend.sdk.component.server.front;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.server.front.model.ActionItem;
import org.talend.sdk.component.server.front.model.ActionList;
import org.talend.sdk.component.server.test.jdbc.JdbcDataSet;

@RunWith(MonoMeecrowave.Runner.class)
public class ActionResourceTest {

    @Inject
    private WebTarget base;

    @Test
    public void index() {
        final ActionList index = base.path("action/index").request(APPLICATION_JSON_TYPE).get(ActionList.class);
        assertEquals(2, index.getItems().size());

        final List<ActionItem> items = new ArrayList<>(index.getItems());
        items.sort(Comparator.comparing(ActionItem::getName));

        final Iterator<ActionItem> it = items.iterator();
        assertAction("proc", "user", "another-test-component-14.11.1986.jarAction", 1, it.next());
        assertAction("chain", "healthcheck", "default", 6, it.next());
    }

    @Test
    public void indexFiltered() {
        final ActionList index = base
                .path("action/index")
                .queryParam("type", "healthcheck")
                .queryParam("family", "chain")
                .request(APPLICATION_JSON_TYPE)
                .get(ActionList.class);
        assertEquals(1, index.getItems().size());
    }

    private void assertAction(final String component, final String type, final String name, final int params,
            final ActionItem value) {
        assertEquals(component, value.getComponent());
        assertEquals(type, value.getType());
        assertEquals(name, value.getName());
        assertEquals(params, value.getProperties().size());
    }
}
