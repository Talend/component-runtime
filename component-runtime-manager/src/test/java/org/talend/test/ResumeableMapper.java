/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.test;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

@PartitionMapper(family = "checkpoint", name = "resumeable-input")
public class ResumeableMapper implements Serializable {

    private final Jsonb jsonb;

    private final RecordBuilderFactory factory;

    private final ResumeableInput.ResumeableConfiguration configuration;

    public ResumeableMapper(Jsonb jsonb, RecordBuilderFactory factory,
            @Option("configuration") ResumeableInput.ResumeableConfiguration configuration) {
        this.jsonb = jsonb;
        this.factory = factory;
        this.configuration = configuration;
    }

    /**
     * @return
     */
    @Assessor
    public long assess() {
        return 0;
    }

    @Split
    public List<ResumeableMapper> split(@PartitionSize long desiredSize) {
        return Collections.singletonList(this);
    }

    /**
     * @return
     */
    @Emitter
    public ResumeableInput createResumeable() {
        return new ResumeableInput(jsonb, factory, configuration);
    }
}
