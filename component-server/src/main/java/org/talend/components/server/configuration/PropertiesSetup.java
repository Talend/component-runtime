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
package org.talend.components.server.configuration;

import java.util.Properties;

import org.apache.meecrowave.Meecrowave;

public class PropertiesSetup implements Meecrowave.ConfigurationCustomizer {

    @Override
    public void accept(final Meecrowave.Builder configuration) {
        if (configuration.getProperties() == null) {
            configuration.setProperties(new Properties());
        }
        configuration.getProperties().putAll(System.getProperties());
        System.setProperties(configuration.getProperties());
    }
}
