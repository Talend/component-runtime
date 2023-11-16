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

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;

@Emitter(family = "lifecycle", name = "countdown")
public class LifecycleCountDownInput implements Serializable, Supplier<List<String>> {

    private final Integer start;

    private transient Iterator<Integer> iterator;

    private static final List<String> lifecycle = new ArrayList<>();

    private final JsonBuilderFactory factory;

    public LifecycleCountDownInput(@Option("start") final Integer start, final JsonBuilderFactory factory) {
        this.start = start;
        this.factory = factory;
    }

    @PostConstruct
    public void start() {
        lifecycle.add("start");
    }

    @PreDestroy
    public void stop() {
        lifecycle.add("stop");
    }

    @Producer
    public JsonObject data() {
        if (iterator == null) {
            List<Integer> data = IntStream.range(0, start).boxed().collect(toList());
            Collections.reverse(data);
            iterator = data.iterator();
        }

        final Integer produce = iterator.hasNext() ? iterator.next() : null;
        lifecycle.add("produce(" + produce + ")");
        return produce == null ? null : this.factory.createObjectBuilder().add("data", produce).build();
    }

    @Override
    public List<String> get() {
        return lifecycle;
    }
}
