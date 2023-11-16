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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;

class TypeConversionTest {

    private final JsonProvider jsonProvider = JsonProvider.provider();

    @Test
    void jsonValueToTalendType() {
        JavaTypesManager javaTypesManager = new JavaTypesManager();
        final TaCoKitGuessSchema guessSchema = new TaCoKitGuessSchema(null, null, null, null, null, null, null);
        assertEquals(javaTypesManager.LONG.getId(), guessSchema.getTalendType(jsonProvider.createValue(1)));
        assertEquals(javaTypesManager.LONG.getId(), guessSchema.getTalendType(jsonProvider.createValue(1L)));
        assertEquals(javaTypesManager.DOUBLE.getId(), guessSchema.getTalendType(jsonProvider.createValue(1.1d)));
        assertEquals(javaTypesManager.BIGDECIMAL.getId(),
                guessSchema.getTalendType(jsonProvider.createValue(new BigDecimal(1.01))));
        assertEquals(javaTypesManager.STRING.getId(), guessSchema.getTalendType(jsonProvider.createValue("azerty")));
        assertEquals(javaTypesManager.BOOLEAN.getId(), guessSchema.getTalendType(JsonValue.TRUE));
        assertEquals(javaTypesManager.BOOLEAN.getId(), guessSchema.getTalendType(JsonValue.FALSE));
        assertEquals(javaTypesManager.OBJECT.getId(),
                guessSchema.getTalendType(jsonProvider.createObjectBuilder().build()));
        assertEquals("", guessSchema.getTalendType(JsonValue.NULL));
    }

}
