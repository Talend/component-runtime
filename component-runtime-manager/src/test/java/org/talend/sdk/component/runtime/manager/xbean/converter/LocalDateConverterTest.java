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
package org.talend.sdk.component.runtime.manager.xbean.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class LocalDateConverterTest {

    @Test
    void ensureItToleratesAnotherFormat() {
        final LocalDateTime localDateTime = LocalDateTime.now();
        assertEquals(localDateTime.toLocalDate(), new LocalDateConverter().toObjectImpl(localDateTime.toString()));
    }

    @Test
    void nativeFormat() {
        final LocalDate localDate = LocalDate.now();
        assertEquals(localDate, new LocalDateConverter().toObjectImpl(localDate.toString()));
    }
}
