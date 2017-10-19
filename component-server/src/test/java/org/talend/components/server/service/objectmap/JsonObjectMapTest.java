// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.service.objectmap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import javax.json.Json;

import org.junit.Test;
import org.talend.component.api.processor.data.ObjectMap;

public class JsonObjectMapTest {

    private final ObjectMap map = new JsonObjectMap(
            Json.createObjectBuilder().add("directString", "test").add("directInt", 1).add("nested", Json.createObjectBuilder()
                    .add("value", "n").add("list", Json.createArrayBuilder().add(1).add(2).build()).build()).build());

    @Test
    public void get() {
        assertEquals("test", map.get("directString"));
        assertEquals(1L, map.get("directInt"));
        assertEquals("n", map.get("nested.value"));
        assertEquals(asList(1L, 2L), map.get("nested.list"));
    }
}
