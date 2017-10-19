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
