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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;

@Emitter(family = "chain", name = "list")
public class ListInput implements Serializable {

    private final Collection<String> list;

    private transient Iterator<String> iterator;

    private JsonBuilderFactory factory;

    private int order = 1;

    public ListInput(@Option("values") final List<String> list, JsonBuilderFactory factory) {
        this.list = list;
        this.factory = factory;
    }

    @Producer
    public JsonObject data() {
        if (iterator == null) {
            iterator = list.iterator();
        }
        return iterator.hasNext()
                ? factory
                        .createObjectBuilder()
                        .add("__talend_internal", factory.createObjectBuilder().add("key", Integer.toString(order++)))
                        .add("data", iterator.next())
                        .build()
                : null;
    }
}
