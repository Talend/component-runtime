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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.runtime.manager.spi.MigrationHandlerListenerExtension;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMigrationHandler implements MigrationHandler {

    private final Collection<MigrationHandlerListenerExtension> listeners = new CopyOnWriteArrayList<>();

    @Getter
    private Map<String, String> configuration;

    /**
     * @param incomingVersion the version of associatedData values.
     * @param incomingData the data sent from the caller. Keys are using the path of the property as in component
     * metadata.
     *
     * @return the set of properties for the current version.
     */
    @Override
    public final Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
        configuration = new HashMap<>(incomingData);
        migrate(incomingVersion);

        return Collections.unmodifiableMap(configuration);
    }

    /**
     * @param incomingVersion the version of associated data values.
     */
    public abstract void migrate(final int incomingVersion);

    /**
     * @param listener the observer that will receive changes
     */
    public final synchronized void registerListener(final MigrationHandlerListenerExtension listener) {
        log.debug("[registerListener] registering {}.", listener.getClass().getName());
        listeners.add(listener);
    }

    /**
     * @param listener the observer that will receive changes
     */
    public final synchronized void unRegisterListener(final MigrationHandlerListenerExtension listener) {
        log.debug("[unRegisterListener] unregistering {}.", listener.getClass().getName());
        listeners.remove(listener);
    }

    /**
     * @param key
     * @param value
     */
    public final void addKey(final String key, final String value) {
        configuration.put(key, value);

        listeners.forEach(e -> e.onAddKey(configuration, key, value));
    }

    /**
     * @param oldKey configuration key existing
     * @param newKey configuration key to rename
     */
    public final void renameKey(final String oldKey, final String newKey) {
        configuration.put(newKey, configuration.get(oldKey));
        configuration.remove(oldKey);

        listeners.forEach(e -> e.onRenameKey(configuration, oldKey, newKey));
    }

    /**
     * @param key configuration key
     */
    public final void removeKey(final String key) {
        configuration.remove(key);

        listeners.forEach(e -> e.onRemoveKey(configuration, key));
    }

    /**
     * @param key configuration key
     * @param newValue new value to set in migration
     */
    public final void changeValue(final String key, final String newValue) {
        final String oldValue = configuration.get(key);
        configuration.put(key, newValue);

        listeners.forEach(e -> e.onChangeValue(configuration, key, oldValue, newValue));
    }

    /**
     * @param key configuration key
     * @param newValue new value to set in migration
     * @param condition predicate that tests current value, if true sets new value
     */
    public final void changeValue(final String key, final String newValue, final Predicate<String> condition) {
        final String oldValue = configuration.get(key);
        if (condition.test(oldValue)) {
            configuration.put(key, newValue);

            listeners.forEach(e -> e.onChangeValue(configuration, key, oldValue, newValue));
        }
    }

    /**
     * @param key configuration key
     * @param updater function to set new value
     */
    public final void changeValue(final String key, final Function<? super String, String> updater) {
        final String oldValue = configuration.get(key);
        final String newValue = updater.apply(oldValue);
        configuration.put(key, newValue);

        listeners.forEach(e -> e.onChangeValue(configuration, key, oldValue, newValue));
    }

    /**
     * @param key configuration key
     * @param updater function to set new value
     * @param condition predicate that tests current value, if true sets new value
     */
    public final void changeValue(final String key, final Function<? super String, String> updater,
            final Predicate<String> condition) {
        final String oldValue = configuration.get(key);
        if (condition.test(oldValue)) {
            final String newValue = updater.apply(oldValue);
            configuration.put(key, newValue);

            listeners.forEach(e -> e.onChangeValue(configuration, key, oldValue, newValue));
        }
    }

}
