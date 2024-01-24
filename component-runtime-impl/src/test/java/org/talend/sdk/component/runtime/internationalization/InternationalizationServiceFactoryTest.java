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
package org.talend.sdk.component.runtime.internationalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.internationalization.Language;

class InternationalizationServiceFactoryTest {

    private final Translate translate = new InternationalizationServiceFactory(Locale::getDefault)
            .create(Translate.class, Translate.class.getClassLoader());

    @Test
    void shortName() {

        assertEquals("The Short Name Works", translate.shortName());
    }

    @Test
    void noConfiguredValue() {

        assertEquals("noConfiguredValue", translate.noConfiguredValue());
    }

    @Test
    void staticValue() {

        assertEquals("FIXED", translate.staticValue());
    }

    @Test
    void dynamic() {

        assertEquals("string<a> and integer<1>", translate.dynamicValue("a", 1));
    }

    @Test
    void customLocale() {

        assertEquals("valeur<france>", translate.customLocale(Locale.FRANCE, "france"));
    }

    @Test
    void objectMethods() {
        assertEquals(translate, translate);
        assertEquals(translate.hashCode(), translate.hashCode());
        final Translate other = new InternationalizationServiceFactory(Locale::getDefault)
                .create(Translate.class, Translate.class.getClassLoader());
        assertNotSame(translate, other);
    }

    @Internationalized
    public interface Translate {

        String shortName();

        String noConfiguredValue();

        String staticValue();

        String dynamicValue(String val, int number);

        String customLocale(@Language Locale lang, String template);
    }
}
