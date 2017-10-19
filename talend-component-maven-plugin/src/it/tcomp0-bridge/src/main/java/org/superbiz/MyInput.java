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
package org.superbiz;

import static java.util.Collections.singletonList;

import static org.talend.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.talend.component.api.component.Icon;
import org.talend.component.api.component.Version;
import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.action.Discoverable;
import org.talend.component.api.configuration.type.DataSet;
import org.talend.component.api.input.Assessor;
import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.PartitionMapper;
import org.talend.component.api.input.Producer;
import org.talend.component.api.input.Split;

@Version
@Icon(FILE_JOB_O)
@PartitionMapper(family = "test", name = "input")
public class MyInput implements Serializable {
    private Configuration configuration;

    public MyInput(@Option("config") Configuration configuration) {
        this.configuration = configuration;
    }

    @Assessor
    public long estimateSize() {
        return 1;
    }

    @Split
    public List<MyInput> split() {
        return singletonList(this);
    }

    @Emitter
    public SomeInput createInput() {
        return new SomeInput(configuration);
    }

    public static class SomeInput implements Serializable {
        private int remaining = 5;

        public SomeInput(Configuration configuration) {
            // no-op
        }

        @Producer
        public Data next() {
            if (remaining-- == 0) {
                return null;
            }
            final Data data = new Data();
            data.value = new Date().toString();
            return data;
        }
    }

    public static class Data {
        private String value;

        public String getValue() {
            return value;
        }
    }

    @DataSet
    @Discoverable
    public static class Configuration {
        @Option("name")
        private String name;
    }
}
