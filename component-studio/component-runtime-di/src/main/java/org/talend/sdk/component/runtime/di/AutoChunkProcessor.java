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
package org.talend.sdk.component.runtime.di;

import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

/*
 * This class is kept for backward compatibility with the studio
 * fixme : remove this class when the studio integration is updated with the class from runtime-manager
 */
public class AutoChunkProcessor extends org.talend.sdk.component.runtime.manager.chain.AutoChunkProcessor {

    public AutoChunkProcessor(final int chunkSize, final Processor processor) {
        super(chunkSize, processor);
    }

    @Override
    public void onElement(final InputFactory ins, final OutputFactory outs) {
        if (processedItemCount == 0) {
            processor.beforeGroup();
        }
        try {
            processor.onNext(ins, outs);
            processedItemCount++;
        } finally {
            if (processedItemCount == chunkSize || chunkSize < 0) {
                processor.afterGroup(outs);
                processedItemCount = 0;
            }
        }
    }
}
