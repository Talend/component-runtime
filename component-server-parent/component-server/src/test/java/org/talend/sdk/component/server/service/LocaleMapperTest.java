/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;

@MonoMeecrowaveConfig
class LocaleMapperTest {

    @Inject
    private LocaleMapper mapper;

    @Test
    void mapFr() {
        assertEquals(Locale.FRENCH, mapper.mapLocale(Locale.FRANCE.toString()));
        assertEquals(Locale.FRENCH, mapper.mapLocale(Locale.CANADA_FRENCH.toString()));
    }

    @Test
    void mapDe() {
        assertEquals(Locale.GERMAN, mapper.mapLocale(Locale.GERMANY.toString()));
        assertEquals(Locale.GERMAN, mapper.mapLocale("de_AU"));
    }

    @Test
    void mapDefault() {
        assertEquals(Locale.ENGLISH, mapper.mapLocale(null));
    }
}
