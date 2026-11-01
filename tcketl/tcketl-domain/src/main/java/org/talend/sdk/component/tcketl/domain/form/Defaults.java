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
package org.talend.sdk.component.tcketl.domain.form;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Fills the values a stored configuration does not have with the defaults the server declares. A value that is present
 * is never touched, so a stored empty string stays empty.
 *
 * <p>
 * A list or map is given its default (a JSON text) only while it has no entry at all, not even the {@code [length]}
 * bound that {@link ConfigurationCodec} writes for a list the user emptied on purpose.
 */
public final class Defaults {

    private Defaults() {
        // static helpers only
    }

    public static ConfigurationValues materialize(final PropertyTree tree, final ConfigurationValues stored) {
        Preconditions.requireNonNull(tree, "tree");
        final Filler filler = new Filler(Preconditions.requireNonNull(stored, "stored"));
        tree.roots().forEach(root -> filler.walk(root, root.path()));
        return filler.result();
    }

    /** The defaults of a single node and its descendants, for an entry that has just been added to a list. */
    static ConfigurationValues materialize(final PropertyNode node, final String path,
            final ConfigurationValues stored) {
        final Filler filler = new Filler(stored);
        filler.walk(node, path);
        return filler.result();
    }

    private static final class Filler extends ValueWalker {

        private final Map<String, String> pending = new LinkedHashMap<>();

        private ConfigurationValues current;

        private Filler(final ConfigurationValues stored) {
            this.current = stored;
        }

        @Override
        int size(final PropertyNode collection, final String path) {
            final boolean map = collection.kind() == PropertyKind.MAP;
            final String[] families = map ? new String[] { path + ".key", path + ".value" } : new String[] { path };
            flush();
            if (Arrays.stream(families).noneMatch(current::hasEntries)) {
                final String json = collection.definition().defaultValue();
                pending.putAll(map ? JsonText.mapEntries(json, path) : JsonText.arrayEntries(json, path));
                flush();
            }
            return current.count(families);
        }

        @Override
        void scalar(final PropertyNode node, final String path) {
            final String value = node.definition().defaultValue();
            if (!value.isEmpty() && !current.contains(path) && !pending.containsKey(path)) {
                pending.put(path, value);
            }
        }

        private ConfigurationValues result() {
            flush();
            return current;
        }

        private void flush() {
            if (!pending.isEmpty()) {
                current = current.withAll(pending);
                pending.clear();
            }
        }
    }
}
