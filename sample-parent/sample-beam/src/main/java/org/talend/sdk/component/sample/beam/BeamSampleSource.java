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
package org.talend.sdk.component.sample.beam;

import java.io.Serializable;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.meta.Documentation;

@Icon(Icon.IconType.DB_INPUT)
@Version(1)
@PartitionMapper(family = "beamsample", name = "Input")
@Documentation("Sample beam input component")
public class BeamSampleSource extends PTransform<PBegin, PCollection<JsonObject>> {

    private final JsonBuilderFactory jsonBuilderFactory;

    private final Configuration configuration;

    public BeamSampleSource(@Option("configuration") final Configuration configuration,
            final JsonBuilderFactory jsonBuilderFactory) {
        this.configuration = configuration;
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public PCollection<JsonObject> expand(final PBegin input) {
        return input.apply(Create.of((Void) null)).apply(ParDo.of(new DoFn<Void, JsonObject>() {

            @ProcessElement
            public void processElement(final ProcessContext context) throws Exception {
                context
                        .output(jsonBuilderFactory
                                .createObjectBuilder()
                                .add(configuration.getColumnName(), configuration.getValue())
                                .build());
            }
        }));
    }

    public static class Configuration implements Serializable {

        private final String columnName;

        private final String value;

        public Configuration(final String columnName, final String value) {
            this.value = value;
            this.columnName = columnName;
        }

        public String getValue() {
            return value;
        }

        public String getColumnName() {
            return columnName;
        }

    }
}
