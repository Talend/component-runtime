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
package org.talend.components.runtime.avro;

import static java.util.Arrays.asList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.talend.component.api.service.Service;
import org.talend.component.api.service.schema.DiscoverSchema;
import org.talend.component.api.service.schema.Type;
import org.talend.components.runtime.manager.ComponentManager;

public class SchemaDiscovererTest {

    @Before
    public void init() {
        ComponentManager.instance().addPlugin(jarLocation(SchemaDiscovererTest.class).getAbsolutePath());
    }

    @After
    public void destroy() {
        ComponentManager.instance().removePlugin(jarLocation(SchemaDiscovererTest.class).getAbsolutePath());
    }

    @Test
    public void populateSchema() {
        final Schema schema = new SchemaDiscoverer().populateSchema("test-classes", "avrotest", "default", "schema", "testid",
                new HashMap<>());
        assertEquals("testid", schema.getName());
        assertEquals(3, schema.getFields().size());
        final Iterator<Schema.Field> fields = schema.getFields().iterator();
        assertField(fields, "value", SchemaBuilder.builder().unionOf().nullType().and().stringType().endUnion().getTypes());
        assertField(fields, "age", SchemaBuilder.builder().unionOf().nullType().and().intType().endUnion().getTypes());
        assertField(fields, "amount", SchemaBuilder.builder().unionOf().nullType().and().doubleType().endUnion().getTypes());
    }

    private void assertField(final Iterator<Schema.Field> fields, final String name, final Collection<Schema> schemas) {
        assertTrue(fields.hasNext());
        final Schema.Field field = fields.next();
        assertEquals(name, field.name());
        assertEquals(new HashSet<>(schemas), new HashSet<>(field.schema().getTypes()));
    }

    @Service
    public static class SchemaService {

        @DiscoverSchema(family = "avrotest")
        public org.talend.component.api.service.schema.Schema find(/* todo */) {
            return new org.talend.component.api.service.schema.Schema(
                    asList(new org.talend.component.api.service.schema.Schema.Entry("value", Type.STRING),
                            new org.talend.component.api.service.schema.Schema.Entry("age", Type.INT),
                            new org.talend.component.api.service.schema.Schema.Entry("amount", Type.DOUBLE)));
        }
    }
}
