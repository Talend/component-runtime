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
package org.talend.sdk.component.runtime.di.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.BIGDECIMAL;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.BOOLEAN;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.BYTE;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.BYTE_ARRAY;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.CHARACTER;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.DATE;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.DOCUMENT;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.DOUBLE;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.DYNAMIC;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.FLOAT;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.INTEGER;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.LIST;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.LONG;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.OBJECT;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.SHORT;
import static org.talend.sdk.component.runtime.di.schema.StudioTypes.STRING;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema.Type;

class StudioTypesTest {

    @Test
    void propertiesValues() {
        assertEquals("id_BigDecimal", BIGDECIMAL);
        assertEquals("id_Boolean", BOOLEAN);
        assertEquals("id_Byte", BYTE);
        assertEquals("id_byte[]", BYTE_ARRAY);
        assertEquals("id_Character", CHARACTER);
        assertEquals("id_Date", DATE);
        assertEquals("id_Double", DOUBLE);
        assertEquals("id_Document", DOCUMENT);
        assertEquals("id_Dynamic", DYNAMIC);
        assertEquals("id_Float", FLOAT);
        assertEquals("id_Integer", INTEGER);
        assertEquals("id_List", LIST);
        assertEquals("id_Long", LONG);
        assertEquals("id_Object", OBJECT);
        assertEquals("id_Short", SHORT);
        assertEquals("id_String", STRING);
    }

    @Test
    void typeFromClass() {
        assertEquals(BIGDECIMAL, StudioTypes.typeFromClass("java.math.BigDecimal"));
        assertEquals(BOOLEAN, StudioTypes.typeFromClass("boolean"));
        assertEquals(BOOLEAN, StudioTypes.typeFromClass("java.lang.Boolean"));
        assertEquals(BYTE, StudioTypes.typeFromClass("byte"));
        assertEquals(BYTE, StudioTypes.typeFromClass("java.lang.Byte"));
        assertEquals(BYTE_ARRAY, StudioTypes.typeFromClass("[B"));
        assertEquals(BYTE_ARRAY, StudioTypes.typeFromClass("byte[]"));
        assertEquals(CHARACTER, StudioTypes.typeFromClass("char"));
        assertEquals(CHARACTER, StudioTypes.typeFromClass("java.lang.Character"));
        assertEquals(DATE, StudioTypes.typeFromClass("java.util.Date"));
        assertEquals(DOUBLE, StudioTypes.typeFromClass("double"));
        assertEquals(DOUBLE, StudioTypes.typeFromClass("java.lang.Double"));
        assertEquals(DYNAMIC, StudioTypes.typeFromClass("routines.system.Dynamic"));
        assertEquals(FLOAT, StudioTypes.typeFromClass("float"));
        assertEquals(FLOAT, StudioTypes.typeFromClass("java.lang.Float"));
        assertEquals(INTEGER, StudioTypes.typeFromClass("int"));
        assertEquals(INTEGER, StudioTypes.typeFromClass("java.lang.Integer"));
        assertEquals(LIST, StudioTypes.typeFromClass("java.util.List"));
        assertEquals(LONG, StudioTypes.typeFromClass("java.lang.Long"));
        assertEquals(LONG, StudioTypes.typeFromClass("long"));
        assertEquals(OBJECT, StudioTypes.typeFromClass("java.lang.Object"));
        assertEquals(SHORT, StudioTypes.typeFromClass("java.lang.Short"));
        assertEquals(SHORT, StudioTypes.typeFromClass("short"));
        assertEquals(STRING, StudioTypes.typeFromClass("java.lang.String"));
        assertEquals(DOCUMENT, StudioTypes.typeFromClass("routines.system.Document"));
        assertThrows(IllegalArgumentException.class,
                () -> StudioTypes.typeFromClass("org.talend.sdk.component.runtime.di.schema.StudioTypesTest"));
    }

    @Test
    void typeFromRecord() {
        assertEquals(LIST, StudioTypes.typeFromRecord(Type.ARRAY));
        assertEquals(BOOLEAN, StudioTypes.typeFromRecord(Type.BOOLEAN));
        assertEquals(BYTE_ARRAY, StudioTypes.typeFromRecord(Type.BYTES));
        assertEquals(DATE, StudioTypes.typeFromRecord(Type.DATETIME));
        assertEquals(DOUBLE, StudioTypes.typeFromRecord(Type.DOUBLE));
        assertEquals(FLOAT, StudioTypes.typeFromRecord(Type.FLOAT));
        assertEquals(INTEGER, StudioTypes.typeFromRecord(Type.INT));
        assertEquals(LONG, StudioTypes.typeFromRecord(Type.LONG));
        assertEquals(OBJECT, StudioTypes.typeFromRecord(Type.RECORD));
        assertEquals(STRING, StudioTypes.typeFromRecord(Type.STRING));
        assertEquals(BIGDECIMAL, StudioTypes.typeFromRecord(Type.DECIMAL));
        assertThrows(IllegalArgumentException.class, () -> StudioTypes.typeFromRecord(null));
    }

    @Test
    void classFromType() {
        assertEquals(java.math.BigDecimal.class, StudioTypes.classFromType(BIGDECIMAL));
        assertEquals(Boolean.class, StudioTypes.classFromType(BOOLEAN));
        assertEquals(Byte.class, StudioTypes.classFromType(BYTE));
        assertEquals(byte[].class, StudioTypes.classFromType(BYTE_ARRAY));
        assertEquals(Character.class, StudioTypes.classFromType(CHARACTER));
        assertEquals(java.util.Date.class, StudioTypes.classFromType(DATE));
        assertEquals(Double.class, StudioTypes.classFromType(DOUBLE));
        assertEquals(Float.class, StudioTypes.classFromType(FLOAT));
        assertEquals(Integer.class, StudioTypes.classFromType(INTEGER));
        assertEquals(java.util.List.class, StudioTypes.classFromType(LIST));
        assertEquals(Long.class, StudioTypes.classFromType(LONG));
        assertEquals(Object.class, StudioTypes.classFromType(OBJECT));
        assertEquals(Short.class, StudioTypes.classFromType(SHORT));
        assertEquals(String.class, StudioTypes.classFromType(STRING));
        assertNull(StudioTypes.classFromType("id_Unknown"));
    }
}