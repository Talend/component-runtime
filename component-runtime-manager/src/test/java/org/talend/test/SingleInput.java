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
package org.talend.test;

import java.io.Serializable;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;

@Emitter(family = "single", name = "input")
public class SingleInput implements Serializable {

    private final JsonBuilderFactory factory;

    private boolean sent;

    public SingleInput(final JsonBuilderFactory factory) {
        this.factory = factory;
    }

    @Producer
    public JsonObject data() {
        if (sent) {
            return null;
        }
        sent = true;
        return factory.createObjectBuilder().add("name", "test").build();
    }
}
