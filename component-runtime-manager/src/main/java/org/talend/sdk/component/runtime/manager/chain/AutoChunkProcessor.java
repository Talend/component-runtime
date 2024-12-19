/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.chain;

import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AutoChunkProcessor implements Lifecycle {

    protected final int chunkSize;

    protected final Processor processor;

    protected int processedItemCount = 0;

    public void onElement(final InputFactory ins, final OutputFactory outs) {
        if (processedItemCount == 0) {
            processor.beforeGroup();
        }
        try {
            processor.onNext(ins, outs);
            processedItemCount++;
        } finally {
            if (processedItemCount == chunkSize) {
                processor.afterGroup(outs);
                processedItemCount = 0;
            }
        }
    }

    public void flush(final OutputFactory outs) {
        if (processedItemCount > 0) {
            processor.afterGroup(outs);
            processedItemCount = 0;
        }
    }

    @Override
    public void stop() {
        processor.stop();
    }

    @Override
    public String plugin() {
        return processor.plugin();
    }

    @Override
    public String rootName() {
        return processor.rootName();
    }

    @Override
    public String name() {
        return processor.name();
    }

    @Override
    public void start() {
        processor.start();
    }
}
