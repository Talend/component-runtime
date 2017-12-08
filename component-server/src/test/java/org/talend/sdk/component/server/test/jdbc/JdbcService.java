/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.talend.sdk.component.api.service.Service;

@Service
public class JdbcService {

    public Connection createConnection(final String driver, final JdbcDataStore dataStore) {
        try {
            Class.forName(driver, true, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Didn't find driver '" + driver + "'", e);
        }
        try {
            return DriverManager.getConnection(dataStore.getUrl(), dataStore.getUsername(), dataStore.getPassword());
        } catch (SQLException e) {
            throw new IllegalStateException("Didn't manage to connect driver using " + dataStore, e);
        }
    }
}
