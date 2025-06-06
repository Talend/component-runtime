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
package org.talend.sdk.component.junit.component;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;

@Emitter(family = "simple", name = "values")
public class Source implements Serializable {

    private final Iterator<String> config;

    public Source(@Option("configuration") final Config config) {
        this.config = config.values.iterator();
    }

    @Producer
    public String data() {
        return config.hasNext() ? config.next() : null;
    }

    public static class Config {

        @Option
        protected List<String> values;
    }
}
