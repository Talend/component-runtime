/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.transform.avro;

import static java.util.stream.Collectors.toList;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import lombok.Getter;

public class JsonIndexedRecord extends GenericData.Record {

    @Getter
    private final JsonObject object;

    public JsonIndexedRecord(final JsonObject object, final Schema schema) {
        super(schema);
        this.object = object;
    }

    @Override
    public void put(final int i, final Object v) {
        throw new UnsupportedOperationException("readonly structure");
    }

    @Override
    public Object get(final int i) {
        final Schema.Field field = getSchema().getFields().get(i);
        return toObject(object.get(field.name()), field.schema());
    }

    private Object toObject(final JsonValue jsonValue, final Schema schema) {
        if (jsonValue == null) {
            return null;
        }
        switch (jsonValue.getValueType()) {
        case TRUE:
            return true;
        case FALSE:
            return false;
        case NUMBER:
            return JsonNumber.class.cast(jsonValue).numberValue();
        case STRING:
            return JsonString.class.cast(jsonValue).getString();
        case ARRAY:
            return jsonValue
                    .asJsonArray()
                    .stream()
                    .map(it -> toObject(jsonValue, schema.getElementType()))
                    .collect(toList());
        case OBJECT:
            return new JsonIndexedRecord(jsonValue.asJsonObject(), schema);
        case NULL:
        default:
            return null;
        }
    }
}
