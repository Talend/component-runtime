/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.runtime;

import static java.util.Collections.singletonList;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.json.JsonBuilderFactory;

import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StitchMapper implements Mapper, Serializable {

    private final String plugin;

    private final String name;

    private final Map<String, String> configuration;

    private final JsonBuilderFactory jsonBuilderFactory;

    private final RecordBuilderFactory factory;

    private final HttpClientFactory clientFactory;

    @Override
    public long assess() {
        return 1L;
    }

    @Override
    public List<Mapper> split(final long desiredSize) {
        return singletonList(this);
    }

    @Override
    public Input create() {
        return new StitchInput(plugin, name, configuration, jsonBuilderFactory, factory, clientFactory);
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public boolean isStream() {
        return false;
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return plugin;
    }

    @Override
    public String name() {
        return name;
    }
}
