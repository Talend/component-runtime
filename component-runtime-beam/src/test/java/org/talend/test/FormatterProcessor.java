/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.test;

import static java.util.Locale.ROOT;

import java.io.Serializable;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "chain", name = "formatter")
public class FormatterProcessor implements Serializable {

    private final JsonBuilderFactory factory;

    public FormatterProcessor(JsonBuilderFactory factory) {
        this.factory = factory;
    }

    @ElementListener
    public void length(@Input("firstName") final JsonObject firstName, @Input("lastName") final JsonObject lastName,
            @Output("formatted-firstName") final OutputEmitter<JsonObject> lowerCase,
            @Output("formatted-lastName") final OutputEmitter<JsonObject> upperCase) {
        final JsonObjectBuilder internal = factory.createObjectBuilder().add("key",
                (firstName == null ? lastName : firstName).getJsonObject("$$internal").getString("key"));
        lowerCase.emit(firstName == null ? null
                : factory
                        .createObjectBuilder()
                        .add("data", firstName.getString("data").toLowerCase(ROOT))
                        .add("$$internal", internal)
                        .build());
        upperCase.emit(lastName == null ? null
                : factory
                        .createObjectBuilder()
                        .add("data", lastName.getString("data").toUpperCase(ROOT))
                        .add("$$internal", internal)
                        .build());
    }
}
