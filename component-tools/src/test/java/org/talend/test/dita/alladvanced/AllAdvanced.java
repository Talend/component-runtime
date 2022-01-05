/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.test.dita.alladvanced;

import lombok.Data;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import java.io.Serializable;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;
import static org.talend.sdk.component.api.configuration.ui.layout.GridLayout.FormType.ADVANCED;

@Documentation("Component without layout")
@Version
@Icon(FILE_JOB_O)
@Processor(family = "dita", name = "AllAdvanced")
public class AllAdvanced {

    public AllAdvanced(@Option("configuration") final AllAdvancedConfig config) {
        // no-op
    }

    @BeforeGroup
    public void beforeGroup() {

    }

    @ElementListener
    public Foo passthrough(final Foo item) {
        return item;
    }

    @AfterGroup
    public void afterGroup() {

    }

    public static class Foo {
    }

    @Data
    @GridLayout(names = ADVANCED, value = { @GridLayout.Row("param1"), @GridLayout.Row("param2"),
            @GridLayout.Row("param3"), @GridLayout.Row("param6") })
    @Documentation("Entry of documentation")
    public static class AllAdvancedConfig {

        @Option
        @Documentation("Param 1 out of dataset")
        public String param1;

        @Option
        @Documentation("Param 2 out of dataset")
        public Integer param2;

        @Option
        @Documentation("Param 3 out of dataset")
        public Complex1 param3;

        @Option
        @Documentation("Param 6 is dataset")
        public NoLayoutDataset param6;
    }

    @Data
    @GridLayout(names = ADVANCED, value = { @GridLayout.Row("param4"), @GridLayout.Row("param5") })
    public static class Complex1 {

        @Option
        @Documentation("Param 4 in complex1")
        public String param4;

        @Option
        @Documentation("Param 5 in complex1")
        public Integer param5;
    }

    @Data
    @DataSet
    @GridLayout(names = ADVANCED,
            value = { @GridLayout.Row("param7"), @GridLayout.Row("param8"), @GridLayout.Row("param11") })
    public static class NoLayoutDataset {

        @Option
        @Documentation("Param 7 is datastore in dataset")
        public NoLayoutDatastore param7;

        @Option
        @Documentation("Param 8 in dataset")
        public Boolean param8;

        @Option
        @Documentation("Param 11 in dataset")
        public Complex2 param11;
    }

    @Data
    @GridLayout(names = ADVANCED, value = { @GridLayout.Row("param9"), @GridLayout.Row("param10") })
    public static class Complex2 {

        @Option
        @Documentation("Param 9 in complex2")
        public String param9;

        @Option
        @Documentation("Param 10 in complex2")
        public Integer param10;
    }

    @Data
    @DataStore
    @GridLayout(names = ADVANCED,
            value = { @GridLayout.Row("param12"), @GridLayout.Row("param13"), @GridLayout.Row("param14") })
    public static class NoLayoutDatastore {

        @Option
        @Documentation("Params 12 in datastore")
        public String param12;

        @Option
        @Documentation("Params 13 in datastore")
        public Integer param13;

        @Option
        @Documentation("Params 14 in datastore")
        public Complex3 param14;
    }

    @Data
    @GridLayout(names = ADVANCED, value = { @GridLayout.Row("param15"), @GridLayout.Row("param16") })
    public static class Complex3 {

        @Option
        @Documentation("Param 15 in complex3")
        public String param15;

        @Option
        @Documentation("Param 16 in complex3")
        public Integer param16;
    }
}
