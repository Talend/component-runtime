/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.server.service.objectmap;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.talend.component.api.processor.data.ObjectMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonObjectMap implements ObjectMap {

    private final JsonObject object;

    @Override
    public Object get(final String location) {
        final int dot = location.indexOf('.');
        if (dot > 0) {
            final String root = location.substring(0, dot);
            if (!object.containsKey(root)) {
                return null;
            }
            return new JsonObjectMap(object.getJsonObject(root)).get(location.substring(dot + 1));
        }
        return object.containsKey(location) ? toObject(object.get(location)) : null;
    }

    @Override
    public ObjectMap getMap(final String location) {
        return ObjectMap.class.cast(get(location));
    }

    @Override
    public Collection<ObjectMap> getCollection(final String location) {
        return Collection.class.cast(get(location));
    }

    @Override
    public Set<String> keys() {
        return object.keySet();
    }

    private Object toObject(final JsonValue json) {
        switch (json.getValueType()) {
        case TRUE:
            return true;
        case FALSE:
            return false;
        case NUMBER:
            return JsonNumber.class.cast(json).numberValue();
        case STRING:
            return JsonString.class.cast(json).getString();
        case ARRAY:
            return JsonArray.class.cast(json).stream().map(this::toObject).collect(toList());
        case OBJECT:
            return new JsonObjectMap(JsonObject.class.cast(json));
        case NULL:
        default:
            return null;
        }
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
