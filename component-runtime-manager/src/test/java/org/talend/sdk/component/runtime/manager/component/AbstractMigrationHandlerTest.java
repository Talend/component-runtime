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
package org.talend.sdk.component.runtime.manager.component;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AbstractMigrationHandlerTest {

    private final AbstractMigrationHandler migrationHandler = new MigrationHandlerTester();

    private final Map<String, String> data = new HashMap<String, String>() {

        {
            put("key0", "value0");
            put("key1", "value1");
            put("key2", "value2");
            put("key3", "value3");
            put("key4", "value4");
            put("key5", "value5");
            put("key6", "value6");
            put("key7", "value7");
        }
    };

    @Test
    void testMigrate() {
        migrationHandler.migrate(2, data);
        final Map<String, String> tested = migrationHandler.getConfiguration();
        Assertions.assertEquals("valeur", tested.get("clé"));
        Assertions.assertNull(tested.get("key0"));
        Assertions.assertEquals("value1", tested.get("config.new.sub.key1"));
        Assertions.assertEquals("22222", tested.get("key2"));
        Assertions.assertEquals("valeur3", tested.get("key3"));
        Assertions.assertEquals("changed4", tested.get("key4"));
        Assertions.assertEquals("value5", tested.get("key5"));
        Assertions.assertEquals("valeur6", tested.get("key6"));
        Assertions.assertEquals("value7", tested.get("key7"));
    }

    @Test
    void testAddKeyWithExistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(3, data));
    }

    @Test
    void testRenameKeyWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(4, data));
    }

    @Test
    void testRemoveKeyWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(5, data));
    }

    @Test
    void testChangeValueWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(6, data));
    }

    @Test
    void testChangeValuePredicateWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(7, data));
    }

    @Test
    void testChangeValuePredicateUpdaterWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(8, data));
    }

    public static class MigrationHandlerTester extends AbstractMigrationHandler {

        @Override
        public void migrate(final int incomingVersion) {
            switch (incomingVersion) {
            case 2:
                addKey("clé", "valeur");
                removeKey("key0");
                renameKey("key1", "config.new.sub.key1");
                changeValue("key2", "22222");
                changeValue("key3", s -> s.replace("value", "valeur"));
                changeValue("key4", "changed4", s -> s.endsWith("4"));
                changeValue("key5", "changed4", s -> s.endsWith("4"));
                changeValue("key6", s -> s.replace("value", "valeur"), s -> s.endsWith("6"));
                changeValue("key7", s -> s.replace("value", "valeur"), s -> s.endsWith("6"));
                break;
            case 3:
                addKey("key0", "excep");
                break;
            case 4:
                renameKey("missingkey", "newkey");
                break;
            case 5:
                removeKey("missingkey");
                break;
            case 6:
                changeValue("missingkey", "22222");
                break;
            case 7:
                changeValue("missingkey", "changed4", s -> s.endsWith("4"));
                break;
            case 8:
                changeValue("missingkey", s -> s.replace("value", "valeur"), s -> s.endsWith("6"));
                break;
            default:
                //
            }
        }
    }
}
