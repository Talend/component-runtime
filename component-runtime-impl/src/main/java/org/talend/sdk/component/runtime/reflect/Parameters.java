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
package org.talend.sdk.component.runtime.reflect;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.json.JsonObject;

import org.talend.sdk.component.api.record.Record;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Parameters {

    public static boolean isGroupBuffer(final Type type) {
        if (!ParameterizedType.class.isInstance(type)) {
            return false;
        }
        final ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        if (!Class.class.isInstance(parameterizedType.getRawType())
                || parameterizedType.getActualTypeArguments().length != 1) {
            return false;
        }
        final Class<?> containerType = Class.class.cast(parameterizedType.getRawType());
        return Collection.class.isAssignableFrom(containerType)
                && asList(Record.class, JsonObject.class).contains(parameterizedType.getActualTypeArguments()[0]);
    }
}
