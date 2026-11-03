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
package org.talend.sdk.component.tcketl.app.async;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Resources that are loaded on first use, off the UI thread, and shared by key. {@link #get} answers at once with
 * the placeholder; the property takes the real value on the UI thread when it is available. A resource that is
 * missing or cannot be loaded keeps the placeholder and is not requested again until {@link #clear()}.
 *
 * <p>
 * Every method MUST be called on the UI thread.
 *
 * @param <K> what identifies a resource
 * @param <V> the resource
 */
public final class LazyResources<K, V> {

    private static final Logger LOGGER = System.getLogger(LazyResources.class.getName());

    private final AsyncRunner async;

    private final V placeholder;

    private final Loader<K, V> loader;

    private final Map<K, ReadOnlyObjectWrapper<V>> entries = new HashMap<>();

    private final List<Cancellable> pending = new ArrayList<>();

    public LazyResources(final AsyncRunner async, final V placeholder, final Loader<K, V> loader) {
        this.async = async;
        this.placeholder = placeholder;
        this.loader = loader;
    }

    public ReadOnlyObjectProperty<V> get(final K key) {
        final ReadOnlyObjectWrapper<V> known = entries.get(key);
        if (known != null) {
            return known.getReadOnlyProperty();
        }
        final ReadOnlyObjectWrapper<V> created = new ReadOnlyObjectWrapper<>(placeholder);
        entries.put(key, created);
        pending.add(async.submit(() -> loader.load(key), loaded -> loaded.ifPresent(created::set),
                failure -> LOGGER.log(Logger.Level.DEBUG, "Cannot load " + key + ", keeping the placeholder",
                        failure)));
        return created.getReadOnlyProperty();
    }

    /** Forgets every resource and abandons the loads in flight. */
    public void clear() {
        pending.forEach(Cancellable::cancel);
        pending.clear();
        entries.clear();
    }

    @FunctionalInterface
    public interface Loader<K, V> {

        /** Blocking; an empty result means the resource does not exist. */
        Optional<V> load(K key) throws Exception;
    }
}
