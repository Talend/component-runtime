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
package org.talend.sdk.component.runtime.beam.design;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.Collection;

import org.talend.sdk.component.design.extension.flows.FlowsFactory;
import org.talend.sdk.component.runtime.output.Branches;

public enum BeamFlowFactory implements FlowsFactory {
    INPUT {

        @Override
        public Collection<String> getInputFlows() {
            return emptySet();
        }

        @Override
        public Collection<String> getOutputFlows() {
            return singleton(Branches.DEFAULT_BRANCH);
        }
    },
    OUTPUT {

        @Override
        public Collection<String> getInputFlows() {
            return singleton(Branches.DEFAULT_BRANCH);
        }

        @Override
        public Collection<String> getOutputFlows() {
            return emptySet();
        }
    };
}
