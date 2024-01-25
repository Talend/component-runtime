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
package org.talend.sdk.component.runtime.di.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Type;

public class StudioTypes {

    public static final String BIGDECIMAL = "id_BigDecimal";

    public static final String BOOLEAN = "id_Boolean";

    public static final String BYTE = "id_Byte";

    public static final String BYTE_ARRAY = "id_byte[]";

    public static final String CHARACTER = "id_Character";

    public static final String DATE = "id_Date";

    public static final String DOUBLE = "id_Double";

    public static final String DOCUMENT = "id_Document"; // unmanaged

    public static final String DYNAMIC = "id_Dynamic";

    public static final String FLOAT = "id_Float";

    public static final String INTEGER = "id_Integer";

    public static final String LIST = "id_List";

    public static final String LONG = "id_Long";

    public static final String OBJECT = "id_Object";

    public static final String SHORT = "id_Short";

    public static final String STRING = "id_String";

    private static final Map<String, String> CLASSES_TO_STUDIO_TYPES = new HashMap<String, String>() {

        {
            put("[B", BYTE_ARRAY);
            put("boolean", BOOLEAN);
            put("byte", BYTE);
            put("byte[]", BYTE_ARRAY);
            put("char", CHARACTER);
            put("double", DOUBLE);
            put("float", FLOAT);
            put("int", INTEGER);
            put("java.lang.Boolean", BOOLEAN);
            put("java.lang.Byte", BYTE);
            put("java.lang.Character", CHARACTER);
            put("java.lang.Double", DOUBLE);
            put("java.lang.Float", FLOAT);
            put("java.lang.Integer", INTEGER);
            put("java.lang.Long", LONG);
            put("java.lang.Object", OBJECT);
            put("java.lang.Short", SHORT);
            put("java.lang.String", STRING);
            put("java.math.BigDecimal", BIGDECIMAL);
            put("java.util.Date", DATE);
            put("java.util.List", LIST);
            put("long", LONG);
            put("routines.system.Dynamic", DYNAMIC);
            put("short", SHORT);
            put("routines.system.Document", DOCUMENT);
        }
    };

    private static final Map<String, Class<?>> STUDIO_TYPES_TO_CLASSES = new HashMap<String, Class<?>>() {

        {
            put(BIGDECIMAL, java.math.BigDecimal.class);
            put(BOOLEAN, Boolean.class);
            put(BYTE, Byte.class);
            put(BYTE_ARRAY, byte[].class);
            put(CHARACTER, Character.class);
            put(DATE, java.util.Date.class);
            put(DOUBLE, Double.class);
            put(FLOAT, Float.class);
            put(INTEGER, Integer.class);
            put(LIST, java.util.List.class);
            put(LONG, Long.class);
            put(OBJECT, Object.class);
            put(SHORT, Short.class);
            put(STRING, String.class);
        }
    };

    private static final Map<Schema.Type, String> RECORD_TYPES_TO_STUDIOS_TYPES = new HashMap<Schema.Type, String>() {

        {
            put(Type.ARRAY, LIST);
            put(Type.BOOLEAN, BOOLEAN);
            put(Type.BYTES, BYTE_ARRAY);
            put(Type.DATETIME, DATE);
            put(Type.DECIMAL, BIGDECIMAL);
            put(Type.DOUBLE, DOUBLE);
            put(Type.FLOAT, FLOAT);
            put(Type.INT, INTEGER);
            put(Type.LONG, LONG);
            put(Type.RECORD, OBJECT);
            put(Type.STRING, STRING);
        }
    };

    public static String typeFromClass(final String clazz) {
        return Optional.ofNullable(CLASSES_TO_STUDIO_TYPES.get(clazz))
                .orElseThrow(() -> new IllegalArgumentException("Unexpected class: " + clazz));
    }

    public static String typeFromRecord(final Schema.Type type) {
        return Optional.ofNullable(RECORD_TYPES_TO_STUDIOS_TYPES.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unexpected type: " + type));
    }

    public static Class<?> classFromType(final String type) {
        return STUDIO_TYPES_TO_CLASSES.get(type);
    }
}
