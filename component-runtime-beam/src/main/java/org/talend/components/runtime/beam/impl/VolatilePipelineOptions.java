// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
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
