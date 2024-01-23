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
package org.talend.test.valid.datasetintwosourceswithonewithadditionalrequired;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;

import javax.json.JsonObject;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;

@Documentation("super my component")
@Version
@Icon(FILE_JOB_O)
@Emitter(family = "test", name = "my2")
public class MyComponent2 implements Serializable {

    public MyComponent2(@Option("configuration") final Config config) {
        // no-op
    }

    @Producer
    public JsonObject stop() {
        return null;
    }

    public static class Config implements Serializable {

        @Option
        @Documentation("...")
        private MyComponent.MyDataSet dataset;

        @Option
        @Required
        private String required;
    }
}
