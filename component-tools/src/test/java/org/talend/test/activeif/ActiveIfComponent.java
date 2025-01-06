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
package org.talend.test.activeif;

import static org.talend.sdk.component.api.configuration.condition.ActiveIf.EvaluationStrategy.LENGTH;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.record.Record;

import lombok.Data;

@Emitter(family = "test", name = "activeif")
public class ActiveIfComponent implements Serializable {

    public ActiveIfComponent(final Configuration configuration) {
        // no-op
    }

    @Producer
    public Record next() {
        return null;
    }

    @Data
    public static class Configuration {

        @Option
        private boolean toggle;

        @Option
        @ActiveIf(target = "toggle", value = "true")
        private String token;

        @Option
        private String type;

        @Option
        @ActiveIf(target = "toggle", value = "true")
        @ActiveIf(target = "type", value = { "mysql", "oracle" })
        private String query;

        @Option
        private boolean advanced;

        @Option
        @ActiveIf(target = "advanced", value = "false")
        @ActiveIf(target = "query", value = "0", evaluationStrategy = LENGTH)
        private String advancedOption;
    }
}
