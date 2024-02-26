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
package org.talend.sdk.component.junit.beam.test;

import java.io.Serializable;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Processor(family = "beamtest", name = "proc")
public class SampleProcessor implements Serializable {

    @ElementListener
    public void onNext(final @Input Sample sample, final @Output OutputEmitter<Sample> success,
            final @Output("reject") OutputEmitter<Reject> reject) {

        success.emit(sample);
        reject.emit(new Reject(sample, "error"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sample {

        private int data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reject {

        private Sample data;

        private String error;
    }
}
