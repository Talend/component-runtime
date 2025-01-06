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
import java.util.List;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Split;

@Version
@Icon(Icon.IconType.DB_INPUT)
@org.talend.sdk.component.api.input.PartitionMapper(family = "family1", name = "input2")
public class PartitionMapper2 implements Serializable {

    public PartitionMapper2(@Option("configuration2") final DataSet2 dataSet) {

    }

    @Assessor
    public long estimateSize() {
        return 0;
    }

    @Split
    public List<PartitionMapper2> split(@PartitionSize final long bundles) {
        return null;
    }

    @Emitter
    public Input createInput() {
        return null;
    }
}
