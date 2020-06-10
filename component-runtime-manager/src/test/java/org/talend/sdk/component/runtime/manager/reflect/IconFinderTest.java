/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.component.Icon;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IconFinderTest {

    private final IconFinder finder = new IconFinder();

    @Test
    void findDirectIcon() {
        assertFalse(finder.findDirectIcon(None.class).isPresent());
        assertEquals("foo", finder.findDirectIcon(Direct.class).get());
    }

    @Test
    void findInDirectIcon() {
        assertFalse(finder.findDirectIcon(Indirect.class).isPresent());
        assertEquals("yes", finder.findIndirectIcon(Indirect.class).get());
    }

    @Test
    void findMetaIcon() {
        assertFalse(finder.findDirectIcon(Indirect.class).isPresent());
        assertEquals("complex_", finder.findIndirectIcon(Meta.class).get());
    }

    @Test
    void findIcon() {
        assertEquals("foo", finder.findIcon(Direct.class));
        assertEquals("yes", finder.findIcon(Indirect.class));
        assertEquals("default", finder.findIcon(None.class));
    }

    @Test
    void helperMethod() {
        {
            final boolean isCustom = finder.isCustom(finder.extractIcon(Direct.class));
            final String name = finder.findIcon(Direct.class);
            assertEquals("foo/true", name + '/' + isCustom);
        }
        {
            final boolean isCustom = finder.isCustom(finder.extractIcon(Meta.class));
            final String name = finder.findIcon(Meta.class);
            assertEquals("complex_/false", name + '/' + isCustom);
        }
    }

    public static class None {
    }

    @Icon(custom = "foo")
    public static class Direct {
    }

    @MyIcon
    public static class Indirect {
    }

    @MetaIcon(MetaIcon.MetaIconValue.COMPLEX)
    public static class Meta {
    }

    @Icon
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface MetaIcon {

        MetaIconValue value();

        // optional but normally not needed EnumOrString type() default "custom";

        enum MetaIconValue {
            SIMPLE,
            COMPLEX;

            public String getKey() {
                return name().toLowerCase(Locale.ROOT) + '_';
            }
        }
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface MyIcon {

        String value() default "yes";
    }
}
