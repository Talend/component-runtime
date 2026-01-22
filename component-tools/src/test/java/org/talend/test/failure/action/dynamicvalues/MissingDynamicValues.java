/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.test.failure.action.dynamicvalues;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;

import javax.json.JsonObject;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.Data;

@Version
@Icon(FILE_JOB_O)
@Processor(family = "test", name = "my")
public class MissingDynamicValues implements Serializable {

    private final Foo foo;

    public MissingDynamicValues(final Foo foo) {
        this.foo = foo;
    }

    @ElementListener
    public JsonObject objectMap(final JsonObject item) {
        return item;
    }

    @Data
    public static class Foo implements Serializable {

        @Option
        @Proposable("TheValues")
        private String value;
    }
}
