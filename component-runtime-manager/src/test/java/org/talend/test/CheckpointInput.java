/*
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package org.talend.test;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.Checkpoint;
import org.talend.sdk.component.api.input.checkpoint.Resume;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Emitter(family = "checkpoint", name = "list-input")
public class CheckpointInput implements Serializable {

    private final JsonBuilderFactory factory;

    private List<Integer> data;

    private transient ListIterator<Integer> iterator;

    private Integer bookmark;

    public CheckpointInput(final JsonBuilderFactory factory) {
        this.factory = factory;
    }

    @PostConstruct
    public void init() {
        log.warn("[init]");
        data = IntStream.range(0, 10).boxed().collect(toList());
    }

    @PreDestroy
    public void destroy() {
        log.warn("[destroy]");
    }

    @Producer
    public JsonObject data() {
        if (iterator == null) {
            iterator = data.listIterator();
        }
        log.warn("[data] previous: {} next: {}.", iterator.previousIndex(), iterator.nextIndex());
        final Integer produced = iterator.hasNext() ? iterator.next() : null;
        bookmark = produced;
        log.warn("[data] produced: {}.", produced);
        return produced == null ? null : factory.createObjectBuilder().add("data", produced).build();
    }

    @Resume
    public void resume(final JsonObject checkpoint) {
        if (checkpoint == null) {
            throw new IllegalArgumentException("No valid checkpoint configuration found.");
        }
        bookmark = checkpoint.getInt("checkpoint");
        log.warn("[resume] resuming at: {} data: {}.", bookmark, data);
        if (bookmark == null) {
            iterator = new ArrayList<Integer>().listIterator();
        } else {
            iterator = data.listIterator(bookmark);
        }
    }

    @Checkpoint
    public JsonObject checkpoint() {
        log.warn("[checkpoint] bookmark: {}.", bookmark);
        return factory.createObjectBuilder().add("checkpoint", bookmark).build();
    }



}
