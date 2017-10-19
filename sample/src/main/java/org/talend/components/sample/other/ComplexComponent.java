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
package org.talend.components.sample.other;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.stream.IntStream;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.action.Proposable;
import org.talend.component.api.configuration.type.DataSet;
import org.talend.component.api.configuration.ui.OptionsOrder;
import org.talend.component.api.configuration.ui.widget.Credential;
import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.Producer;
import org.talend.component.api.service.Service;
import org.talend.component.api.service.completion.DynamicValues;
import org.talend.component.api.service.completion.Values;

import lombok.Data;

@Emitter(family = "complex", name = "demo")
public class ComplexComponent implements Serializable {

    private final ComplexDataSet dataset;

    public ComplexComponent(@Option("dataset") final ComplexDataSet dataset) {
        this.dataset = dataset;
    }

    @Producer
    public String value() {
        return "";
    }

    @Data
    @DataSet("complicated")
    public static class Credentials implements Serializable {

        @Option
        private String username;

        @Option
        @Credential
        private String password;
    }

    @Data
    @DataSet("complicated")
    @OptionsOrder({ "path", "credentials" })
    public static class ComplexDataSet implements Serializable {

        @Option
        private Credentials credentials;

        @Option
        @Proposable("path")
        private String path;
    }

    @Service
    public static class PathService {

        @DynamicValues(family = "complex", value = "path")
        public Values find(@Option("value") final String value) {
            return new Values(IntStream.range(1, 11).mapToObj(i -> "/opt/sample/file_" + i + ".txt")
                                       .map(Values.Item::new).collect(toList()));
        }
    }
}
