/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.sample;

import static java.util.Collections.emptyMap;

import java.util.HashMap;

import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.CountingSuccessListener;
import org.talend.sdk.component.runtime.manager.chain.ExecutionChainBuilder;
import org.talend.sdk.component.runtime.manager.chain.ToleratingErrorHandler;

public class Main {

    public static void main(final String[] args) {
        // tag::main[]
        try (final ComponentManager manager = ComponentManager.instance()) {
            ExecutionChainBuilder.start().withConfiguration("SampleJob", true)
                .fromInput("sample", "reader", 2, new HashMap<String, String>() {

                    {
                        put("file", "/tmp/input.csv");
                    }
                }).toProcessor("sample", "mapper", 1, emptyMap())
                .toProcessor(null, "sample", "writer", 1, new HashMap<String, String>() {

                    {
                        put("file", "/tmp/output.csv");
                    }
                }).create(manager, plugin -> null, new CountingSuccessListener(), new ToleratingErrorHandler(0)).get()
                .execute();
        }
        // end::main[]
    }
}
