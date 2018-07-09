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
package org.talend.sdk.component.runtime.beam.impl;

import org.apache.beam.sdk.transforms.DoFn;
import org.joda.time.Instant;
import org.talend.sdk.component.runtime.output.OutputFactory;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OutputReceiver implements DoFn.OutputReceiver {

    private final OutputFactory outputs;

    private final String branch;

    @Override
    public void output(final Object output) {
        outputs.create(branch).emit(output);
    }

    @Override
    public void outputWithTimestamp(final Object output, final Instant timestamp) {
        outputs.create(branch).emit(output);
    }
}
