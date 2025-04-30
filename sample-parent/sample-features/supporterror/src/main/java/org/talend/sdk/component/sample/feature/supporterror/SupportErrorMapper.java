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
package org.talend.sdk.component.sample.feature.supporterror;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.meta.Documentation;

@Version(1)
@Icon(value = Icon.IconType.CUSTOM, custom = "mapper")
@PartitionMapper(name = "SupportErrorMapper", infinite = false)
@Documentation("Doc: default SupportErrorMapper documentation without Internationalization.")
public class SupportErrorMapper implements Serializable {

    private SupportErrorInput.InputConfig config;

    public SupportErrorMapper(final @Option("configin") SupportErrorInput.InputConfig config) {
        this.config = config;
    }

    @Assessor
    public long estimateSize() {
        return 1500L;
    }

    @Split
    public List<SupportErrorMapper> split(@PartitionSize final int desiredNbSplits) {
        return Collections.singletonList(this);
    }

    @Emitter
    public SupportErrorInput createSource() {

        return new SupportErrorInput(this.config);
    }

}
