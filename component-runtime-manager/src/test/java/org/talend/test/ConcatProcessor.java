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
package org.talend.test;

import java.io.Serializable;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "processor", name = "concat")
public class ConcatProcessor implements Serializable {

    private final JsonBuilderFactory factory;

    public ConcatProcessor(JsonBuilderFactory factory) {
        this.factory = factory;
    }

    @ElementListener
    public void cat(@Input("str1") final JsonObject str1, @Input("str2") final JsonObject str2,
            @Output final OutputEmitter<JsonObject> concat) {

        if (str1 == null && str2 == null) {
            return;
        }

        final JsonObjectBuilder builder = factory.createObjectBuilder();
        if (str1 != null) {
            str1.keySet().forEach(k -> builder.add(k, str1.get(k)));
        }
        if (str2 != null) {
            str2.keySet().forEach(k -> builder.add(k, str2.get(k)));
        }

        concat.emit(builder.build());
    }
}
