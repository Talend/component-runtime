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
package org.talend.components.runtime.manager.test;

import lombok.Getter;

import java.util.List;
import java.util.Map;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.type.DataSet;

public class MethodsHolder {

    public void primitives(@Option("url") final String url, @Option final String defaultName, @Option("port") final int port) {
        // no-op
    }

    public void collections(@Option("urls") final List<String> urls, @Option("ports") final List<Integer> ports,
            @Option("mapping") final Map<String, String> mapping) {
        // no-op
    }

    public void object(final Config implicit, @Option("prefixed") final Config prefixed) {
        // no-op
    }

    public void nested(final ConfigOfConfig value) {
        // no-op
    }

    public void array(final Array value) {
        // no-op
    }

    @Getter
    public static class Array {

        @Option
        private String[] urls;
    }

    @Getter
    @DataSet("test")
    public static class Config {

        @Option
        private List<String> urls;

        @Option
        private Map<String, String> mapping;
    }

    @Getter
    public static class ConfigOfConfig {

        @Option
        private List<Config> multiple;

        @Option
        private Map<String, Config> keyed;

        @Option
        private Config direct;

        @Option
        private String passthrough;
    }
}
