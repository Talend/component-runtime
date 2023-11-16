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
package org.talend.test;

import java.io.Serializable;
import java.util.Locale;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "processor", name = "formatter")
public class FormatterProcessor implements Serializable {

    private final JsonBuilderFactory factory;

    private final boolean lowerCase;

    public FormatterProcessor(@Option("lowerCase") boolean lowerCase, JsonBuilderFactory factory) {
        this.factory = factory;
        this.lowerCase = lowerCase;
    }

    @ElementListener
    public void format(final JsonObject data, @Output("formatted") final OutputEmitter<JsonObject> formatted) {

        if (data == null) {
            return;
        }

        final JsonObjectBuilder builder = factory.createObjectBuilder();
        if (lowerCase) {
            data.keySet().forEach(k -> builder.add(k, data.getString(k).toLowerCase(Locale.ROOT)));
        } else {
            data.keySet().forEach(k -> builder.add(k, data.getString(k).toUpperCase(Locale.ROOT)));
        }
        formatted.emit(builder.build());
    }
}
