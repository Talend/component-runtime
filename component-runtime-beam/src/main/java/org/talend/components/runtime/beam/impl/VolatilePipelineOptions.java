/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.runtime.beam.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.beam.sdk.PipelineRunner;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.display.DisplayData;

class VolatilePipelineOptions implements PipelineOptions {

    private Class<? extends PipelineRunner<?>> runner = Class.class.cast(PipelineRunner.class);

    private CheckEnabled checkEnabled = CheckEnabled.OFF;

    private String tempLocation = System.getProperty("java.io.tmpdir");

    private String jobName = "job";

    private Map<String, Map<String, Object>> runtimeOptions = new HashMap<>();

    private long id;

    @Override
    public <T extends PipelineOptions> T as(final Class<T> kls) {
        return kls.isInstance(this) ? kls.cast(this) : null;
    }

    @Override
    public Class<? extends PipelineRunner<?>> getRunner() {
        return runner;
    }

    @Override
    public void setRunner(final Class<? extends PipelineRunner<?>> kls) {
        this.runner = kls;
    }

    @Override
    public CheckEnabled getStableUniqueNames() {
        return checkEnabled;
    }

    @Override
    public void setStableUniqueNames(final CheckEnabled enabled) {
        this.checkEnabled = enabled;
    }

    @Override
    public String getTempLocation() {
        return tempLocation;
    }

    @Override
    public void setTempLocation(final String value) {
        this.tempLocation = value;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public Map<String, Map<String, Object>> outputRuntimeOptions() {
        return runtimeOptions;
    }

    @Override
    public long getOptionsId() {
        return id;
    }

    @Override
    public void setOptionsId(final long id) {
        this.id = id;
    }

    @Override
    public void populateDisplayData(final DisplayData.Builder builder) {
        // no-op
    }

    @Override
    public String getUserAgent() {
        // Stub
        return null;
    }

    @Override
    public void setUserAgent(String arg0) {
        // Stub
    }
}
