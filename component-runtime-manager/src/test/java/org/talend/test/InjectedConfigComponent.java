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
package org.talend.test;

import static java.util.Collections.singleton;

import java.io.Serializable;
import java.util.Iterator;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.service.configuration.Configuration;

import lombok.Data;

@Emitter(family = "config", name = "injected")
public class InjectedConfigComponent implements Serializable {

    private final Iterator<String> values;

    private final JsonBuilderFactory builder;

    public InjectedConfigComponent(@Configuration("java") final Version version, final JsonBuilderFactory builder) {
        this.values = singleton(version.getValue()).iterator();
        this.builder = builder;
    }

    @Producer
    public JsonObject next() {
        if (values.hasNext()) {
            return builder.createObjectBuilder().add("value", values.next()).build();
        }
        return null;
    }

    @Data
    public static class Version {

        @Option("version")
        private String value;
    }
}
