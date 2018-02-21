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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "lifecycle", name = "square")
public class LifecycleSquareProcessor implements Serializable, Supplier<List<String>> {

    private static final List<String> lifecycle = new ArrayList<>();

    @PostConstruct
    public void start() {
        lifecycle.add("start");
    }

    @PreDestroy
    public void stop() {
        lifecycle.add("stop");
    }

    @BeforeGroup
    public void beforeGroup() {
        lifecycle.add("beforeGroup");
    }

    @AfterGroup
    public void afterGroup() {
        lifecycle.add("afterGroup");
    }

    @ElementListener
    public void onNext(@Input final Integer value, @Output final OutputEmitter<Integer> result) {

        lifecycle.add("onNext(" + value + ")");
        result.emit(value * value);
    }

    @Override
    public List<String> get() {
        return lifecycle;
    }
}
