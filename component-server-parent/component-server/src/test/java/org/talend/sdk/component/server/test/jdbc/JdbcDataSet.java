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

import java.io.Serializable;
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@DataSet("jdbc")
@AllArgsConstructor
@NoArgsConstructor
@Documentation("Jdbc dataset test doc.")
@Version(value = -1/* to not break tests */, migrationHandler = JdbcDataSet.Migration.class)
public class JdbcDataSet implements Serializable {

    @Option
    @Documentation(value = "Documentation for Connection.", tooltip = true)
    private JdbcDataStore connection;

    @Option
    @Min(1) // not empty
    @Documentation(value = "Documentation for Driver.", tooltip = true)
    private String driver;

    @Option
    @Min(1) // not empty
    @Documentation(value = "Documentation for Query.", tooltip = true)
    private String query;

    @Option
    @Min(1) // not 0 == infinite
    @Documentation(value = "Documentation for Timeout.", tooltip = true)
    private int timeout;

    public static class Migration implements MigrationHandler {

        @Override
        public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
            if (incomingVersion == -3) {
                throw new RuntimeException("Error thrown for testing!");
            } else if (incomingVersion == -4) {
                throw new NullPointerException();
            }
            incomingData.put("migrated", "true");
            incomingData.put("size", Integer.toString(incomingData.size()));
            return incomingData;
        }
    }
}
