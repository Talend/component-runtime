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

import java.io.Serializable;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.constraint.Min;
import org.talend.component.api.configuration.type.DataSet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@DataSet("jdbc")
@AllArgsConstructor
@NoArgsConstructor
public class JdbcDataSet implements Serializable {

    @Option
    private JdbcDataStore connection;

    @Option
    @Min(1) // not empty
    private String driver;

    @Option
    @Min(1) // not empty
    private String query;

    @Option
    @Min(1) // not 0 == infinite
    private int timeout;
}
