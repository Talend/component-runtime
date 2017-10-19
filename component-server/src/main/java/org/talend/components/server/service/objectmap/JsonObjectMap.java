// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
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
