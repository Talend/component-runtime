/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.form.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import org.h2.tools.Server;

@ApplicationScoped
public class SampleDatabaseCreator {

    private Connection connection;

    private Server server;

    public void createDatabase(@Initialized(ApplicationScoped.class) @Observes final Object init) throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "");
        try (final Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE PUBLIC.USER(ID INTEGER NOT NULL, NAME CHAR(25), PRIMARY KEY (ID))");
            statement.execute("INSERT INTO PUBLIC.USER(ID, NAME) VALUES(1, 'Jimmy')");
            statement.execute("INSERT INTO PUBLIC.USER(ID, NAME) VALUES(2, 'Gary')");
            statement.execute("INSERT INTO PUBLIC.USER(ID, NAME) VALUES(3, 'Led')");
        }

        server = Server.createTcpServer().start(); // expose it over jdbc:h2:tcp://localhost/mem:test
    }

    public void releaseDatabase(@Destroyed(ApplicationScoped.class) @Observes final Object destroy) throws SQLException {
        connection.close();
        server.stop();
    }
}
