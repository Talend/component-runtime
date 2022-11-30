/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.test.custom;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.Pattern;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.constraint.Uniques;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;

@Processor(family = "custom", name = "noop")
public class CustomProcessor implements Serializable {

    @Option
    DataSetCustom dataset;

    public CustomProcessor (@Option("configuration") final DataSetCustom dataset) {
        this.dataset = dataset;
    }

    @ElementListener
    public Record onElement(final Record data) {
        return data;
    }

    @DataSet("dataset")
    static class DataSetCustom {

        @Option
        @Required
        private Connection connection;

        @Option
        @Min(100)
        @Max(150)
        private int limit;
    }

    @Version(migrationHandler = Connection.ConnectionMigration.class)
    @DataStore("Connection")
    static class Connection {

        @Option
        @Pattern("^https?://.*")
        private String url0;

        @Option
        @Required
        @Pattern("^https?://.*")
        private String url1;

        @Option
        @Required
        private String username;

        @Option
        @Required
        @ActiveIf(target = "username", evaluationStrategy = ActiveIf.EvaluationStrategy.CONTAINS, value = "undx")
        private String password;

        @Option
        @Uniques
        private List<String> uniqVals;

        @Option
        private boolean checkbox1;
        @Option
        private boolean checkbox2;

        @Option
        @ActiveIf(target = "checkbox1", value = "true")
        private ValueEval valueEval= ValueEval.VALUE_2;

        @Option
        @Required
        @ActiveIfs(operator = ActiveIfs.Operator.AND, value = {
                @ActiveIf(target = "checkbox1", value = "true"),
                @ActiveIf(target = "checkbox2", value = "true")
        })
        private String activedIfs;

        enum ValueEval {
            VALUE_1,
            VALUE_2,
            VALUE_3;
        }

        static class ConnectionMigration implements MigrationHandler {

            @Override
            public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
                incomingData.put("url", "http://migrated");
                return incomingData;
            }
        }
    }
}
