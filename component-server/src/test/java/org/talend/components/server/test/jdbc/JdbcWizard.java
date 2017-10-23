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
package org.talend.components.server.test.jdbc;

import static lombok.AccessLevel.PRIVATE;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.service.Service;
import org.talend.component.api.service.wizard.Wizard;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class JdbcWizard { // just a code style, no enclosing needed

    @Data
    public static class DriverSelector {

        @Option
        private String driver;
    }

    @Data
    public static class QueryConfiguration {

        @Option
        private String query;

        @Option
        private int timeout;
    }

    @Service
    public static class JdbcWizardCompanionService {

        @Wizard(family = "jdbc", value = "dataset")
        public JdbcDataSet onSubmit(@Option("driver") final DriverSelector first, @Option("store") final JdbcDataStore second,
                @Option("query") final QueryConfiguration query) {
            return new JdbcDataSet(second, first.getDriver(), query.getQuery(), query.getTimeout());
        }
    }
}
