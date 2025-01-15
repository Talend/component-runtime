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

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "chain", name = "count")
public class CounterProcessor implements Serializable {

    private final boolean multiple;

    private final JsonBuilderFactory factory;

    private int previous = 0;

    public CounterProcessor(@Option("multiple") final boolean multiple, final JsonBuilderFactory factory) {
        this.multiple = multiple;
        this.factory = factory;
    }

    @ElementListener
    public void length(final Object input, @Output final OutputEmitter<Integer> main,
            @Output final OutputEmitter<JsonObject> mainJson, @Output("rejected") final OutputEmitter<String> rejects) {
        final String data = input.toString();
        if (data.contains("reject")) {
            rejects.emit(data);
        } else if (multiple) {
            mainJson.emit(factory.createObjectBuilder().add("cumulatedSize", previous += data.length()).build());
            mainJson.emit(factory.createObjectBuilder().add("cumulatedSize", previous += data.length()).build());
        } else {
            main.emit(previous += data.length());
        }
    }
}
