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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.IMAGE_SVG_XML_TYPE;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.THEME_DARK;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.THEME_LIGHT;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentManager;

@MonoMeecrowaveConfig
class IconResolverTest {

    public static final String PLUGIN_DB_INPUT = "db-input";

    public static final String MEDIA_TYPE_PNG = "image/png";

    @Inject
    private ComponentManager manager;

    @Inject
    IconResolver resolver;

    Container plugin;

    @BeforeEach
    void setup() {
        System.setProperty("talend.studio.version", "802");
        plugin = manager.getContainer().find("jdbc-component").get();
        if (plugin.get(IconResolver.Cache.class) != null) {
            plugin.set(IconResolver.Cache.class, new IconResolver.Cache());
        }
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("talend.studio.version");
        System.clearProperty("talend.component.server.icon.svg.support");
    }

    @Test
    void studioThemedIconWithSvg() {
        System.setProperty("talend.component.server.icon.svg.support", "true");
        resolver.init();
        //
        IconResolver.Icon icon = resolver.resolve(plugin, "logo", THEME_DARK);
        assertNotNull(icon);
        assertEquals(IMAGE_SVG_XML_TYPE, icon.getType());
        assertEquals(THEME_DARK, icon.getTheme());
        //
        icon = resolver.resolve(plugin, PLUGIN_DB_INPUT, THEME_DARK);
        assertNotNull(icon);
        assertEquals(IMAGE_SVG_XML_TYPE, icon.getType());
        assertEquals(THEME_DARK, icon.getTheme());
        //
        icon = resolver.resolve(plugin, PLUGIN_DB_INPUT, THEME_LIGHT);
        assertNotNull(icon);
        assertEquals(MEDIA_TYPE_PNG, icon.getType());
        assertEquals(THEME_LIGHT, icon.getTheme());
    }

    @Test
    void studioThemedIconWithoutSvg() {
        System.setProperty("talend.component.server.icon.svg.support", "false");
        resolver.init();
        //
        IconResolver.Icon icon = resolver.resolve(plugin, "logo", "");
        assertNull(icon);
        //
        icon = resolver.resolve(plugin, PLUGIN_DB_INPUT, "");
        assertNotNull(icon);
        assertEquals(MEDIA_TYPE_PNG, icon.getType());
        assertEquals("", icon.getTheme());
        //
        icon = resolver.resolve(plugin, PLUGIN_DB_INPUT, THEME_LIGHT);
        assertNotNull(icon);
        assertEquals(MEDIA_TYPE_PNG, icon.getType());
        assertEquals(THEME_LIGHT, icon.getTheme());
        //
        icon = resolver.resolve(plugin, PLUGIN_DB_INPUT, THEME_DARK);
        assertNotNull(icon);
        assertEquals(MEDIA_TYPE_PNG, icon.getType());
        assertEquals("", icon.getTheme());
    }

}