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
package org.talend.sdk.component.junit;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;

class JoinInputFactoryTest {

    @Test
    void iterationEmpty() {
        final JoinInputFactory factory = new JoinInputFactory();
        assertFalse(factory.hasMoreData());
    }

    @Test
    void iteration() {
        final JoinInputFactory factory = new JoinInputFactory().withInput("__default__", singleton("test"));
        assertTrue(factory.hasMoreData());
        assertEquals("test", factory.read("__default__"));
        assertFalse(factory.hasMoreData());
    }

    @Test
    void jsonp() {
        final JoinInputFactory factory = new JoinInputFactory()
                .withInput("__default__", singleton(Json.createObjectBuilder().add("test", "foo").build()));
        assertTrue(factory.hasMoreData());
        final Object main = factory.read("__default__");
        assertTrue(Record.class.isInstance(main));
        assertEquals("AvroRecord{delegate={\"test\": \"foo\"}}", main.toString());
        assertFalse(factory.hasMoreData());
    }
}
