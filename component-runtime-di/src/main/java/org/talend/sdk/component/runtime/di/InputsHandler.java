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

import javax.json.bind.Jsonb;

import org.talend.sdk.component.runtime.output.InputFactory;

public class InputsHandler extends BaseIOHandler {

    public InputsHandler(final Jsonb jsonb) {
        super(jsonb);
    }

    public InputFactory asInputFactory() {
        return name -> {
            final String actualName = getActualName(name);
            final BaseIOHandler.IO ref = connections.get(actualName);
            if (ref == null || ref.getValue() == null || ref.getValue().get() == null) {
                return null;
            }
            Object value = ref.getValue().get();

            return javax.json.JsonValue.class.isInstance(value) ? javax.json.JsonValue.class.cast(value)
                    : jsonb.fromJson(jsonb.toJson(value), javax.json.JsonValue.class);
        };
    }

    public <T> void setInputValue(final String name, final T value) {
        IO input = connections.get(getActualName(name));
        if (input != null) {
            input.getValue().set(value);
        }
    }

}
