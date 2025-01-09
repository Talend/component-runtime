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
package org.talend.sdk.component.sample;

import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.Job;

public class Main {

    public static void main(final String[] args) {
        // tag::main[]
        try (final ComponentManager manager = ComponentManager.instance()) {
            Job
                    .components()
                    .component("reader", "sample://reader?file=/tmp/input.csv")
                    .component("mapper", "sample://mapper")
                    .component("writer", "sample://writer?file=/tmp/output.csv")
                    .connections()
                    .from("reader")
                    .to("mapper")
                    .from("mapper")
                    .to("writer")
                    .build()
                    .run();

        }
        // end::main[]
    }
}
