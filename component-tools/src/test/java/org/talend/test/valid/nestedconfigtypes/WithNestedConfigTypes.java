/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.test.valid.nestedconfigtypes;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;

import java.io.Serializable;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;
import static org.talend.sdk.component.api.configuration.ui.layout.GridLayout.FormType.ADVANCED;

@Documentation("super my component")
@Version
@Icon(FILE_JOB_O)
@Emitter(family = "test", name = "WithNestedConfigTypes")
public class WithNestedConfigTypes implements Serializable {

    public WithNestedConfigTypes(@Option("configuration") final Conf config) {
        // no-op
    }

    @Producer
    public Record stop() {
        return null;
    }

    @GridLayout({ @GridLayout.Row("input"), @GridLayout.Row("confWithDataset"), })
    public static class Conf implements Serializable {

        @Option
        @Documentation("the input value")
        private String input;

        @Option
        @Documentation("config with dataset")
        private ConfWithDataset confWithDataset;

    }

    @GridLayout({ @GridLayout.Row("dataset") })
    private static class ConfWithDataset implements Serializable {

        @Option
        private MyDataSet dataset;
    }

    @GridLayout({ @GridLayout.Row("datastore") })
    public static class ConfigWithDatastore implements Serializable {

        @Option
        @Documentation("...")
        private MyDataStore datastore;
    }

    @DataStore("MyDataStore")
    @GridLayout({ @GridLayout.Row("user") })
    @GridLayout(names = ADVANCED, value = { @GridLayout.Row("advanced_ds") })
    public static class MyDataStore implements Serializable {

        @Option
        @Documentation("the user to log in")
        private String user;

        @Option
        @Documentation("Advanced in datastore")
        private String advanced_ds;
    }

    @DataSet("MyDataSet")
    @GridLayout({ @GridLayout.Row("configWithDatastore") })
    @GridLayout(names = ADVANCED, value = { @GridLayout.Row("advanced") })
    public static class MyDataSet implements Serializable {

        @Option
        @Documentation("config with datastore")
        private ConfigWithDatastore configWithDatastore;

        @Option
        @Documentation("Advanced parameter")
        private String advanced;

    }
}
