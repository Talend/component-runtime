/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.sample.feature.actionsinforms;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.action.Validable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@GridLayout({
        @GridLayout.Row({ "dataset" })
})
@Documentation("Configuration used by the emitter connector.")
public class InputConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Option
    @Documentation("The dataset part of the configuration.")
    @Updatable(value = SimpleService.UPDATABLE, parameters = "..", after = "input1")
    private Dataset dataset = new Dataset();

    // -----------------------
    // Datastore
    // -----------------------
    @Data
    @DataStore("datastore")
    @Documentation("Connection information (simple example).")
    public static class Datastore implements Serializable {

    }

    // -----------------------
    // Dataset
    // -----------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @DataSet("dataset")
    @GridLayout({
            @GridLayout.Row({ "datastore" }),
            @GridLayout.Row({ "suggestable" }),
            @GridLayout.Row({ "input1" }),
            @GridLayout.Row({ "input2" }),
            @GridLayout.Row({ "generateExceptions" })
    })
    @Documentation("Dataset selecting a simple resource.")
    public static class Dataset implements Serializable {

        private static final long serialVersionUID = 1L;

        @Option
        @Documentation("The datastore.")
        private Datastore datastore = new Datastore();

        @Option
        @Suggestable(value = SimpleService.SUGGESTABLE, parameters = "../..")
        @Documentation("An option with a @Suggestable.")
        private String suggestable;

        @Option
        @Documentation("Valid if it starts by 'ok', The validation throws an exception if it contains 'exception'.")
        @Validable(value = SimpleService.ASYNC_VALIDATION)
        private String input1;

        @Option
        @Documentation("This option is set by the @Update action.")
        private String input2;

        @Option
        @Documentation("When checked, all actions throw an exception.")
        private boolean generateExceptions;
    }
}