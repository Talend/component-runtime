/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.sample.feature.availableoutput.service;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.outputs.AvailableOutputFlows;
import org.talend.sdk.component.sample.feature.availableoutput.output.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class AvaiableoutputService {

    public final static String SECOND_FLOW_NAME = "second";

    public final static String THIRD_FLOW_NAME = "third";

    @AvailableOutputFlows("active_flows")
    public Collection<String> getAvailableFlows(final @Option Configuration config) {
        List<String> flows = new ArrayList<>();
        if (config.getShowSecond()) {
            flows.add(SECOND_FLOW_NAME);
        }
        if (config.getShowThird()) {
            flows.add(THIRD_FLOW_NAME);
        }
        return flows;
    }

//    @AvailableOutputFlows("active_flows_2")
//    public Collection<String> getAvailableFlows2(final @Option Configuration2 config) {
//        List<String> flows = new ArrayList<>();
//        if (config.getShowSecond()) {
//            flows.add(SECOND_FLOW_NAME);
//        }
//        if (config.getShowThird()) {
//            flows.add(THIRD_FLOW_NAME);
//        }
//        return flows;
//    }
}