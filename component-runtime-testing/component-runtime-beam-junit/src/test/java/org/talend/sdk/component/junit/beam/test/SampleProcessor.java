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
package org.talend.sdk.component.junit.beam.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Processor(family = "beamtest", name = "proc")
public class SampleProcessor implements Serializable {

    public static final Collection<String> STACK = new ArrayList<>();

    @ElementListener
    public Sample onNext(final Sample sample) {
        return sample;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sample {

        private int data;
    }
}
