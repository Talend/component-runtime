/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di;

import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.runtime.output.OutputFactory;

// todo: move to Record
public class OutputsHandler extends BaseIOHandler {

    public OutputsHandler(final Jsonb jsonb) {
        super(jsonb);
    }

    public OutputFactory asOutputFactory() {
        return name -> value -> {
            final BaseIOHandler.IO ref = connections.get(getActualName(name));
            if (ref != null && value != null) {
                final String jsonValue = JsonValue.class.isInstance(value) ? JsonValue.class.cast(value).toString()
                        : jsonb.toJson(value);
                ref.add(jsonb.fromJson(jsonValue, ref.getType()));
            }
        };
    }

}
