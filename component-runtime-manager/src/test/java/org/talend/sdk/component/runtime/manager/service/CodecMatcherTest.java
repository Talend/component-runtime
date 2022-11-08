/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.http.ContentType;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.runtime.manager.service.http.codec.CodecMatcher;

class CodecMatcherTest {

    private static Map<String, Encoder> codecMap;

    private CodecMatcher<Encoder> matcher;

    @BeforeAll
    static void init() {
        final Map<String, Encoder> encoderMap = new HashMap<String, Encoder>() {

            {
                put("*/*", new DefaultEncoder());
                put("*/json", new JsonEncoder());
                put("application/*+json", new TalendJsonEncoder());
                put("application/talend+json", new TalendJsonEncoder());
                put("application/json", new JsonEncoder());
            }
        };
        codecMap = new TreeMap<String, Encoder>(new MediaTypeComparator(new ArrayList<>(encoderMap.keySet()))) {

            {
                putAll(encoderMap);
            }
        };
    }

    @BeforeEach
    void before() {
        matcher = new CodecMatcher<>();
    }

    @Test
    void selectWithExactMatch() {
        final Encoder jsonEncoder = matcher.select(codecMap, "application/json");
        assertTrue(JsonEncoder.class.isInstance(jsonEncoder));
        assertEquals("application/json", jsonEncoder.getClass().getAnnotation(ContentType.class).value());

        final Encoder talendEncoder = matcher.select(codecMap, "application/talend+json");
        assertTrue(TalendJsonEncoder.class.isInstance(talendEncoder));
        assertEquals("application/talend+json", talendEncoder.getClass().getAnnotation(ContentType.class).value());

        final Encoder defaultEncoder = matcher.select(codecMap, "*/*");
        assertTrue(DefaultEncoder.class.isInstance(defaultEncoder));
    }

    @Test
    void selectWithtRegex() {
        final Encoder jsonEncoder = matcher.select(codecMap, "foo/json");
        assertTrue(JsonEncoder.class.isInstance(jsonEncoder));
        assertEquals("application/json", jsonEncoder.getClass().getAnnotation(ContentType.class).value());

        final Encoder talendEncoder = matcher.select(codecMap, "application/foo+json");
        assertTrue(TalendJsonEncoder.class.isInstance(talendEncoder));
        assertEquals("application/talend+json", talendEncoder.getClass().getAnnotation(ContentType.class).value());
    }

    @Test
    void selectWithNoType() {
        final Encoder encoder = matcher.select(codecMap, null);
        assertTrue(DefaultEncoder.class.isInstance(encoder));
    }

    @Test
    void wildcardMatching() {
        assertTrue(DefaultEncoder.class.isInstance(matcher.select(codecMap, "foo/bar")));
        assertTrue(DefaultEncoder.class.isInstance(matcher.select(codecMap, "foo/bar+test")));
    }

    @Test
    void noMatchingEntry() {
        assertThrows(IllegalStateException.class, () -> matcher.select(new HashMap<String, Encoder>(codecMap) {

            {
                remove("*/*");
            }
        }, "no/matching"));
    }

    @ContentType("application/json")
    public static class JsonEncoder implements Encoder {

        @Override
        public byte[] encode(final Object value) {
            return new byte[0];
        }
    }

    @ContentType("application/talend+json")
    public static class TalendJsonEncoder implements Encoder {

        @Override
        public byte[] encode(final Object value) {
            return new byte[0];
        }
    }

    public static class DefaultEncoder implements Encoder {

        @Override
        public byte[] encode(final Object value) {
            return new byte[0];
        }
    }
}
