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
package org.talend.components.server.test.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.talend.component.api.service.Service;

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
