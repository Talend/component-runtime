/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import java.util.ArrayList;
import java.util.Collection;

import javax.json.JsonObject;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "store", name = "collect")
public class InMemCollector implements Serializable {

    public static final Collection<JsonObject> OUTPUTS = new ArrayList<>();

    @ElementListener
    public void capture(final JsonObject output) {
        OUTPUTS.add(output);
    }

    public static Collection<JsonObject> getShadedOutputs(final ClassLoader loader, final String location) {
        try {
            return Collection.class
                    .cast(loader
                            .loadClass("org.talend.test.generated." + location + ".InMemCollector")
                            .getField("OUTPUTS")
                            .get(null));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
