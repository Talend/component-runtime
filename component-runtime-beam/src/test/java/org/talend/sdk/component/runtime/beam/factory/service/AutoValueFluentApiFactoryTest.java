/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.factory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.HashMap;

import javax.json.JsonObject;

import org.apache.beam.sdk.options.ValueProvider;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.beam.factory.service.io.CustomJdbcIO;

class AutoValueFluentApiFactoryTest {

    @Test
    void createJDBCInput() throws Exception {
        final CustomJdbcIO.Read read =
                new AutoValueFluentApiFactory().create(CustomJdbcIO.Read.class, "read", new HashMap<String, Object>() {

                    {
                        // we support flat config (primitives) and custom ones for advanced objects
                        //
                        // note that since we reuse PropertyEditors you can also register a converter
                        // but it must be global and not per component!
                        put("dataSourceConfiguration.driverClassName", "org.h2.Driver");
                        put("dataSourceConfiguration.url", "jdbc:h2:mem:test");
                        put("dataSourceConfiguration.username", "sa");
                        put("query", "select * from user");
                        put("rowMapper", (CustomJdbcIO.RowMapper<JsonObject>) resultSet -> null);
                    }
                });
        final CustomJdbcIO.DataSourceConfiguration dataSourceConfiguration =
                readField(read, "dataSourceConfiguration", CustomJdbcIO.DataSourceConfiguration.class);
        assertNotNull(dataSourceConfiguration);
        assertEquals("org.h2.Driver", readField(dataSourceConfiguration, "driverClassName", String.class));
        assertEquals("jdbc:h2:mem:test", readField(dataSourceConfiguration, "url", String.class));
        assertEquals("sa", readField(dataSourceConfiguration, "username", String.class));
        assertNull(readField(dataSourceConfiguration, "password", String.class));
        assertEquals("select * from user", readField(read, "query", ValueProvider.class).get());
        assertNotNull(readField(read, "rowMapper", CustomJdbcIO.RowMapper.class));
    }

    private <T> T readField(final Object root, final String name, final Class<T> type) throws Exception {
        final Field field = root.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(root));
    }
}
