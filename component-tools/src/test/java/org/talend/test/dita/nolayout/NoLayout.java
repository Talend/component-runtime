/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.test.dita.nolayout;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.Data;

@Documentation("Component without layout")
@Version
@Icon(FILE_JOB_O)
@Processor(family = "dita", name = "NoLayout")
public class NoLayout {

    public NoLayout(@Option("configuration") final NoLayoutConfig config) {
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
    @Documentation("Entry of documentation")
    public static class NoLayoutConfig {

        public NoLayoutConfig() {

        }

        @Option
        @DefaultValue("Hello")
        @Documentation("Param 1 out of dataset")
        public String param1;

        @Option
        @Documentation("Param 2 out of dataset")
        public Integer param2 = 50;

        @Option
        @Documentation("Param 3 out of dataset")
        public Complex1 param3;

        @Option
        @Documentation("Param 6 is dataset")
        public NoLayoutDataset param6;
    }

    @Data
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
    public static class NoLayoutDatastore {

        @Option
        @Documentation("Params 12 in datastore")
        public String param12 = "a value";

        @Option
        @ActiveIf(target = "param12", value = { "a value" })
        @Documentation("Params 13 in datastore")
        public Integer param13;

        @Option
        @Documentation("Params 14 in datastore")
        public Complex3 param14;
    }

    @Data
    public static class Complex3 {

        @Option
        @Documentation("Param 15 in complex3")
        public String param15;

        @Option
        @Documentation("Param 16 in complex3")
        public Integer param16;
    }
}
