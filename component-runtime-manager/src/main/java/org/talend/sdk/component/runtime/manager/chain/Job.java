/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import org.talend.sdk.component.runtime.manager.chain.internal.DSLParser;
import org.talend.sdk.component.runtime.manager.chain.internal.JobImpl;

import lombok.Data;

public interface Job {

    static ComponentBuilder components() {
        return new JobImpl.NodeBuilderImpl();
    }

    interface ComponentBuilder {

        NodeBuilder component(final String id, String uri);
    }

    interface NodeBuilder extends ComponentBuilder {

        NodeBuilder property(String name, Object value);

        FromBuilder connections();
    }

    interface FromBuilder {

        ToBuilder from(String id, String branch);

        default ToBuilder from(String id) {
            return from(id, "__default__");
        }

    }

    interface ToBuilder {

        Builder to(String id, String branch);

        default Builder to(String id) {
            return to(id, "__default__");
        }
    }

    interface Builder extends FromBuilder {

        ExecutorBuilder build();
    }

    interface ExecutorBuilder {

        ExecutorBuilder property(String name, Object value);

        void run();
    }

    @Data
    class Component {

        private final String id;

        private boolean isSource = false;

        private final DSLParser.Step node;
    }

    @Data
    class Connection {

        private final Component node;

        private final String branch;
    }

    @Data
    class Edge {

        private final Connection from;

        private final Connection to;
    }

}
