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
package org.talend.sdk.component.server.test.jdbc;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.service.schema.FixedSchema;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Icon(Icon.IconType.DB_INPUT)
@Version(value = 2, migrationHandler = JdbcInput.JdbcTestHandler.class)
@Emitter(family = "jdbc", name = "input")
@FixedSchema("jdbc_discover_schema")
public class JdbcInput implements Serializable {

    private final JdbcDataSet dataset;

    private final JdbcService service;

    // in real impl it can be moved to a State delegate
    private transient Connection connection;

    private transient PreparedStatement statement;

    private transient ResultSet resultSet;

    private transient Map<String, Supplier<Object>> mapper = new HashMap<>();

    public JdbcInput(@Option("configuration") final JdbcDataSet dataset, final JdbcService service) {
        this.dataset = dataset;
        this.service = service;
    }

    @PostConstruct
    public void createConnection() {
        connection = service.createConnection(dataset.getDriver(), dataset.getConnection());
    }

    @Producer
    public Map<String, Object> nextRow() throws SQLException {
        if (resultSet == null) { // execute the query and prepare the mapping to pass to next processor
            statement = connection.prepareStatement(dataset.getQuery());
            resultSet = statement.executeQuery();
            final ResultSetMetaData metaData = resultSet.getMetaData();
            mapper.clear();
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                final int idx = i;
                final String name = metaData.getColumnLabel(i);
                switch (metaData.getColumnType(i)) { // todo: real impl would use a mapper and not do it here
                    case Types.VARCHAR:
                        mapper.put(name, () -> {
                            try {
                                return resultSet.getString(idx);
                            } catch (final SQLException e) {
                                log.warn(e.getMessage(), e);
                                return null;
                            }
                        });
                        break;
                    case Types.DOUBLE:
                        mapper.put(name, () -> {
                            try {
                                return resultSet.getDouble(idx);
                            } catch (final SQLException e) {
                                log.warn(e.getMessage(), e);
                                return null;
                            }
                        });
                        break;
                    default:
                        mapper.put(name, () -> {
                            try {
                                return resultSet.getObject(idx);
                            } catch (final SQLException e) {
                                log.warn(e.getMessage(), e);
                                return null;
                            }
                        });
                }
            }
        }
        return resultSet.next() ? map() : null;
    }

    @PreDestroy
    public void closeConnection() {
        ofNullable(resultSet).ifPresent(closeable -> {
            try {
                closeable.close();
            } catch (final SQLException e) {
                log.warn(e.getMessage(), e);
            }
        });
        ofNullable(statement).ifPresent(closeable -> {
            try {
                closeable.close();
            } catch (final SQLException e) {
                log.warn(e.getMessage(), e);
            }
        });
        ofNullable(connection).ifPresent(closeable -> {
            try {
                closeable.close();
            } catch (final SQLException e) {
                log.warn(e.getMessage(), e);
            }
        });
    }

    private Map<String, Object> map() {
        return mapper.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public static class JdbcTestHandler implements MigrationHandler {

        @Override
        public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
            incomingData.put("migrated", "true");
            return incomingData;
        }
    }
}
