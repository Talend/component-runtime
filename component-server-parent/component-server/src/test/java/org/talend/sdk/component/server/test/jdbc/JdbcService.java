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
package org.talend.sdk.component.server.test.jdbc;

import static java.util.Collections.singletonMap;
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

@Service
public class JdbcService {

    @Service
    private I18n i18n;

    public Connection createConnection(final String driver, final JdbcDataStore dataStore) {
        try {
            Class.forName(driver, true, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Didn't find driver '" + driver + "'", e);
        }
        try {
            return DriverManager.getConnection(dataStore.getUrl(), dataStore.getUsername(), dataStore.getPassword());
        } catch (final SQLException e) {
            throw new IllegalStateException("Didn't manage to connect driver using " + dataStore, e);
        }
    }

    @Action("i18n")
    public Map<String, String> i18n() {
        return singletonMap("value", i18n.read());
    }

    @Action("custom")
    public Map<String, String> test(@Option("enum") final MyEnum myEnum) {
        if (myEnum == MyEnum.FAIL) {
            throw new IllegalArgumentException("this action failed intentionally");
        }
        return singletonMap("value", myEnum.name());
    }

    @Action("encrypted")
    public Map<String, String> testEncrypted(@Option("configuration") final JdbcDataStore conf) {

        return new HashMap<String, String>() {

            {
                put("url", conf.getUrl());
                put("username", conf.getUsername());
                put("password", conf.getPassword());
            }
        };
    }

    @DiscoverSchema("jdbc_discover_schema")
    public Schema guessSchema(final RecordBuilderFactory factory) {
        return factory
                .newSchemaBuilder(RECORD)
                .withEntry(factory
                        .newEntryBuilder()
                        .withName("array")
                        .withType(ARRAY)
                        .withElementSchema(factory.newSchemaBuilder(STRING).build())
                        .build())
                .build();
    }

    @DiscoverSchemaExtended("jdbc_processor_schema")
    public Schema guessProcessorSchema(final Schema incoming, final JdbcConfig config, final String branch) {
        final RecordBuilderFactory factory = new RecordBuilderFactoryImpl("jdbc");
        return factory.newSchemaBuilder(incoming)
                .withEntry(factory.newEntryBuilder()
                        .withName(branch)
                        .withType(STRING)
                        .build())
                .withEntry(factory.newEntryBuilder()
                        .withName("driver")
                        .withType(STRING)
                        .withComment(config.getDriver())
                        .build())
                .build();
    }

    @DynamicDependencies("jdbc-deps")
    public List<String> getExportDependencies(@Option("configuration") final JdbcDataSet dataset) {
        final List<String> dependencies = new ArrayList<>();
        switch (dataset.getDriver()) {
            case "derby":
                dependencies.add("org.apache.derby:derbyclient:jar:10.12.1.1");
                break;
            case "mysql":
                dependencies.add("com.mysql:mysql-connector-j:jar:8.0.33");
                break;
            case "zorglub":
                dependencies.add("org.zorglub:zorglub-client:1.0.0");
                dependencies.add("org.bouncycastle:bcpkix-jdk15to18:jar:4.5.8");
                dependencies.add("org.json:json:jar:2.0.0");
            default:
        }
        return dependencies;
    }

    public enum MyEnum {
        V1,
        V2,
        FAIL
    }

    @Internationalized
    public interface I18n {

        String read();
    }
}
