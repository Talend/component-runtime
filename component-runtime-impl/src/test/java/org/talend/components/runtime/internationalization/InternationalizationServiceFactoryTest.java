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
package org.talend.components.runtime.internationalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.Locale;

import org.junit.Test;
import org.talend.component.api.internationalization.Internationalized;
import org.talend.component.api.internationalization.Language;

public class InternationalizationServiceFactoryTest {

    private final Translate translate = new InternationalizationServiceFactory().create(Translate.class,
            Translate.class.getClassLoader());

    @Test
    public void noConfiguredValue() {

        assertEquals("noConfiguredValue", translate.noConfiguredValue());
    }

    @Test
    public void staticValue() {

        assertEquals("FIXED", translate.staticValue());
    }

    @Test
    public void dynamic() {

        assertEquals("string<a> and integer<1>", translate.dynamicValue("a", 1));
    }

    @Test
    public void customLocale() {

        assertEquals("valeur<france>", translate.customLocale(Locale.FRANCE, "france"));
    }

    @Test
    public void objectMethods() {
        assertEquals(translate, translate);
        assertEquals(translate.hashCode(), translate.hashCode());
        final Translate other = new InternationalizationServiceFactory().create(Translate.class,
                Translate.class.getClassLoader());
        assertNotSame(translate, other);
    }

    @Internationalized
    public interface Translate {

        String noConfiguredValue();

        String staticValue();

        String dynamicValue(String val, int number);

        String customLocale(@Language Locale lang, String template);
    }
}
