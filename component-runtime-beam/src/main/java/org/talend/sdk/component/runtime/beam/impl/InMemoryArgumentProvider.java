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
package org.talend.sdk.component.runtime.beam.impl;

import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.state.State;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.reflect.DoFnInvoker;
import org.apache.beam.sdk.transforms.splittabledofn.RestrictionTracker;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class InMemoryArgumentProvider implements DoFnInvoker.ArgumentProvider {

    private final PipelineOptions options;

    private final ContextImplGenericsHolder contextImplGenericsHolder = new ContextImplGenericsHolder();

    @Setter
    private InputFactory inputs;

    @Setter
    private OutputFactory outputs;

    @Override
    public BoundedWindow window() {
        return GlobalWindow.INSTANCE;
    }

    @Override
    public PipelineOptions pipelineOptions() {
        return options;
    }

    @Override
    public DoFn.ProcessContext processContext(final DoFn doFn) {
        return contextImplGenericsHolder.newContext(options, inputs, outputs);
    }

    @Override
    public DoFn.StartBundleContext startBundleContext(final DoFn doFn) {
        return contextImplGenericsHolder.newStartContext(options);
    }

    @Override
    public DoFn.FinishBundleContext finishBundleContext(final DoFn doFn) {
        return contextImplGenericsHolder.newFinishContext(options, outputs);
    }

    @Override
    public DoFn.OnTimerContext onTimerContext(final DoFn doFn) {
        throw new UnsupportedOperationException("Unsupported on timer usage");
    }

    @Override
    public RestrictionTracker<?> restrictionTracker() {
        throw new UnsupportedOperationException("Unsupported restriction tracker usage");
    }

    @Override
    public State state(final String stateId) {
        throw new UnsupportedOperationException("Unsupported state tracker usage");
    }

    @Override
    public Timer timer(final String timerId) {
        throw new UnsupportedOperationException("Unsupported timer tracker usage");
    }
}
