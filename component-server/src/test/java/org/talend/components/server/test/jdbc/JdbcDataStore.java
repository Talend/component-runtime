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
import org.talend.component.api.configuration.ui.widget.Credential;
import org.talend.component.api.configuration.type.DataStore;

import lombok.Data;

@Data
@DataStore("jdbc")
public class JdbcDataStore implements Serializable {

    @Option
    private String url;

    @Option
    private String username;

    @Option
    @Credential
    private String password;
}
