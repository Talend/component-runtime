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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.MigrationListener;
import org.talend.sdk.component.runtime.manager.spi.MigrationHandlerListenerExtension;

class AbstractMigrationHandlerTest {

    private final AbstractMigrationHandler migrationHandler = new MigrationHandlerTester();

    private MigrationHandlerListenerExtension listener;

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

    @BeforeEach
    void setup() {
        listener = new MigrationListener();
        migrationHandler.registerListener(listener);
    }

    @AfterEach
    void tearDown() {
        migrationHandler.unRegisterListener(listener);
    }

    @Test
    void testMigrate() {
        migrationHandler.migrate(2, data);
        final Map<String, String> tested = migrationHandler.getConfiguration();
        Assertions.assertEquals(8, tested.size());
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

    @Test
    void testSplitProperty() {
        final Map<String, String> tested = migrationHandler.migrate(9, data);
        Assertions.assertEquals(9, tested.size());
        Assertions.assertEquals(tested.get("config.new.sub.key1.val0"), "value1");
        Assertions.assertEquals(tested.get("config.new.sub.key1.val1"), "value1");
    }

    @Test
    void testSplitPropertyWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(10, data));
    }

    @Test
    void testMergeProperties() {
        final Map<String, String> tested = migrationHandler.migrate(11, data);
        Assertions.assertEquals(6, tested.size());
        Assertions.assertEquals(tested.get("config.new.sub.key1"), "value1-value2-value3");
    }

    @Test
    void testMergePropertiesWithInexistingKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> migrationHandler.migrate(12, data));
    }

    public static class MigrationHandlerTester extends AbstractMigrationHandler {

        @Override
        public void migrate(final int incomingVersion) {
            try {
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
                case 9:
                    splitProperty("key1", Arrays.asList("config.new.sub.key1.val0", "config.new.sub.key1.val1"));
                    break;
                case 10:
                    splitProperty("config.new", Arrays.asList("config.new.val0", "config.new.val1"));
                    break;
                case 11:
                    mergeProperties(Arrays.asList("key1", "key2", "key3"), "config.new.sub.key1");
                    break;
                case 12:
                    mergeProperties(Arrays.asList("key1", "key22", "key3"), "config.new.sub.key1");
                    break;
                default:
                    //
                }
            } catch (MigrationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void doSplitProperty(final String oldKey, final List<String> newKeys) {
            final String val = configuration.get(oldKey);
            newKeys.stream().forEach(k -> configuration.put(k, val));
            configuration.remove(oldKey);
        }

        @Override
        public void doMergeProperties(final List<String> oldKeys, final String newKey) {
            List<String> vals = oldKeys.stream().map(k -> configuration.get(k)).collect(Collectors.toList());
            configuration.put(newKey, vals.stream().collect(Collectors.joining("-")));
            oldKeys.stream().forEach(k -> configuration.remove(k));
        }
    }
}
