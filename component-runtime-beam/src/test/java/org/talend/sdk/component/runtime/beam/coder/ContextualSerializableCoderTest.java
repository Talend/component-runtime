/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;

import org.apache.beam.sdk.util.SerializableUtils;
import org.junit.jupiter.api.Test;

import lombok.RequiredArgsConstructor;

class ContextualSerializableCoderTest {

    @Test
    void roundTrip() { // this test is equivalent of the normal SerializableCoder but ensures the roundtrip works
        final Ser ser = SerializableUtils
                .ensureSerializableByCoder(ContextualSerializableCoder.of(Ser.class, "test"), new Ser(5), "test");
        assertEquals(5, ser.value);
    }

    @RequiredArgsConstructor
    public static class Ser implements Serializable {

        private final int value;
    }
}
