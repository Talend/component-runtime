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
package org.talend.sdk.component.sample.feature.availableoutput.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.outputs.AvailableOutputFlows;
import org.talend.sdk.component.sample.feature.availableoutput.output.Configuration;
import org.talend.sdk.component.sample.feature.availableoutput.output.ConfigurationTwo;

@Service
public class AvaiableoutputService  implements Serializable {

    public static final String SECOND_FLOW_NAME = "second";

    public static final String THIRD_FLOW_NAME = "third";

    public static final String SECOND_FLOW_NAME2 = "second-2";

    public static final String THIRD_FLOW_NAME2 = "third-2";

    @AvailableOutputFlows("output-flow1")
    public List<String> getAvailableFlows(final @Option("configuration") Configuration config) {
        List<String> flows = new ArrayList<>();
        if (config.isShowSecond()) {
            flows.add(SECOND_FLOW_NAME);
        }
        if (config.isShowThird()) {
            flows.add(THIRD_FLOW_NAME);
        }
        return flows;
    }

    @AvailableOutputFlows("output-flow2")
    public List<String> getAvailableFlows2(final @Option("configuration") ConfigurationTwo config) {
        List<String> flows = new ArrayList<>();
        if (config.isShowSecond2()) {
            flows.add(SECOND_FLOW_NAME2);
        }
        if (config.isShowThird2()) {
            flows.add(THIRD_FLOW_NAME2);
        }
        return flows;
    }
}