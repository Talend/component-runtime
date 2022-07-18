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
package org.talend.sdk.component.api.service.schema;

import java.io.StringReader;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.talend.sdk.component.api.record.Schema;

import lombok.Data;

/**
 * Use to declare an input for a ProcessSchema service
 * {@link org.talend.sdk.component.api.service.schema.ProcessSchema}.
 * (See unit test on Component-Server, with JdbcService)
 */
@Data
public class InputSchema {

    private String schema;

    private String outputBranch;

    public Schema getInputSchema(final Function<JsonObject, Schema> converter) {
        try (StringReader reader = new StringReader(this.schema);
                JsonReader jsonReader = Json.createReader(reader)) {
            final JsonObject jsonObject = jsonReader.readObject();
            return converter.apply(jsonObject);
        }
    }

}
