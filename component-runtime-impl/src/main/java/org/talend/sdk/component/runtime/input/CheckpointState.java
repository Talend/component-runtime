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
package org.talend.sdk.component.runtime.input;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.spi.JsonbProvider;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckpointState {

    public static final String CHECKPOINT_KEY = "$checkpoint";

    public static final String VERSION_KEY = "__version";

    @JsonbProperty(VERSION_KEY)
    private int version;

    private Object state;

    public JsonObject toJson() {
        final Jsonb jsonb = JsonbProvider.provider().create().build();
        return Json.createObjectBuilder()
                .add(CHECKPOINT_KEY, Json.createObjectBuilder(jsonb.fromJson(jsonb.toJson(state), JsonObject.class))
                        .add(VERSION_KEY, version)
                        .build())
                .build();
    }
}
