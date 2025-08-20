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
package org.talend.sdk.component.feature.form.input;

import java.io.Serializable;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.feature.form.config.ComplexConfig;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Emitter(name = "Input")
@Documentation("A connector that returns only 1 record reflecting the configuration.")
public class SimpleInput implements Serializable {

    private final ComplexConfig config;

    private final RecordBuilderFactory recordBuilderFactory;

    private boolean done;

    public SimpleInput(final ComplexConfig config,
            final RecordBuilderFactory recordBuilderFactory) {
        this.config = config;
        this.recordBuilderFactory = recordBuilderFactory;
    }

    @Producer
    public Record next() {
        if (done) {
            return null;
        }
        done = true;

        Builder builder = recordBuilderFactory.newRecordBuilder();
        builder.withString("configuration", config.toString());
        builder.withString("fixedString", "A fixed string");
        builder.withInt("fixedInt", 100);
        builder.withBoolean("fixedBoolean", true);

        return builder.build();
    }

}
