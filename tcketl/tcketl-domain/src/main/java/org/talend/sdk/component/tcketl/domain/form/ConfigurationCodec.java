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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Turns the values of a form into the flat map the runtime reads.
 *
 * <p>
 * The rules, which keep the map both valid for the runtime and lossless for the designer:
 * <ul>
 * <li>a text value is written as is; an empty value is left out, except for a text whose default is not empty, which
 * is written empty so that reopening the form does not bring the default back;</li>
 * <li>the entries of a list of values, and the keys and values of a map, are always written, empty or not, since
 * leaving one out would shorten the list;</li>
 * <li>a list that has no entry but would get a default is written as {@code list[length]=0}, the bound that tells
 * {@link Defaults} the list was emptied on purpose;</li>
 * <li>keys that the property tree does not describe, such as the technical {@code $maxRecords}, the
 * {@code __version} of a saved configuration or the leftovers of another component version, are kept untouched,
 * except the UI-only keys the runtime rejects ({@code $driver_name} and the like).</li>
 * </ul>
 * The values of invisible properties are written like any other: hiding is a display concern.
 */
public final class ConfigurationCodec {

    private static final String LENGTH_SUFFIX = "[length]";

    private static final String VERSION_KEY = "__version";

    /** How a stored key relates to the property tree. */
    public enum KeyClass {
        /** The tree describes the property. */
        KNOWN,
        /** Not described, yet meaningful to the runtime: a {@code $} option, {@code __version}, a list length. */
        TECHNICAL,
        /** Not described and not recognised, for instance a property of another component version. */
        UNKNOWN,
        /** A key made for the UI, which the runtime refuses. */
        UI_COMPANION
    }

    private ConfigurationCodec() {
        // static helpers only
    }

    /** The flat map to give the runtime, sorted by key. */
    public static Map<String, String> encode(final PropertyTree tree, final ConfigurationValues values) {
        Preconditions.requireNonNull(tree, "tree");
        Preconditions.requireNonNull(values, "values");
        final Encoder encoder = new Encoder(values);
        tree.roots().forEach(root -> encoder.walk(root, root.path()));
        return Collections.unmodifiableMap(encoder.result());
    }

    public static KeyClass classify(final PropertyTree tree, final String key) {
        if (isUiCompanion(key)) {
            return KeyClass.UI_COMPANION;
        }
        if (tree.find(key).isPresent()) {
            return KeyClass.KNOWN;
        }
        return isTechnical(key) ? KeyClass.TECHNICAL : KeyClass.UNKNOWN;
    }

    /** The runtime ignores, with a warning, a key that has a {@code $} followed later by {@code _name}. */
    static boolean isUiCompanion(final String key) {
        final int dollar = key.indexOf('$');
        return dollar >= 0 && key.indexOf("_name", dollar) > dollar;
    }

    private static boolean isTechnical(final String key) {
        if (key.endsWith(LENGTH_SUFFIX)) {
            return true;
        }
        for (final String segment : key.split("\\.")) {
            if (segment.startsWith("$") || VERSION_KEY.equals(segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * An entry that makes an otherwise empty element exist: an empty text when the element has one, else a neutral
     * value for its first value, since the runtime cannot convert an empty string to a number or an enum.
     */
    static Optional<Map.Entry<String, String>> marker(final PropertyNode element, final String path) {
        final Map<String, PropertyNode> leaves = new LinkedHashMap<>();
        collectLeaves(element, path, leaves);
        return leaves
                .entrySet()
                .stream()
                .filter(leaf -> leaf.getValue().kind() == PropertyKind.STRING)
                .findFirst()
                .or(() -> leaves.entrySet().stream().findFirst())
                .map(leaf -> Map.entry(leaf.getKey(), neutralValue(leaf.getValue())));
    }

    private static String neutralValue(final PropertyNode leaf) {
        switch (leaf.kind()) {
            case BOOLEAN:
                return "false";
            case NUMBER:
                return "0";
            case ENUM:
                return leaf.definition().constraints().enumValues().stream().findFirst().orElse("");
            default:
                return "";
        }
    }

    private static void collectLeaves(final PropertyNode node, final String path,
            final Map<String, PropertyNode> leaves) {
        for (final PropertyNode child : node.children()) {
            final String childPath = child.concretePath(path, 0);
            if (child.kind().isScalar()) {
                leaves.put(childPath, child);
            } else if (child.kind() == PropertyKind.OBJECT) {
                collectLeaves(child, childPath, leaves);
            }
        }
    }

    private record ObjectArray(PropertyNode array, String path, int size) {
    }

    private static final class Encoder extends ValueWalker {

        private final ConfigurationValues values;

        private final Set<String> visited = new HashSet<>();

        private final Map<String, String> encoded = new TreeMap<>();

        private final List<ObjectArray> objectArrays = new ArrayList<>();

        private Encoder(final ConfigurationValues values) {
            this.values = values;
        }

        @Override
        int size(final PropertyNode collection, final String path) {
            return countEntries(values, collection, path);
        }

        @Override
        void collection(final PropertyNode node, final String path, final int size) {
            if (!node.kind().isArray()) {
                return;
            }
            if (size > 0 && node.kind() == PropertyKind.ARRAY_OF_OBJECT) {
                objectArrays.add(new ObjectArray(node, path, size));
            }
            final String length = path + LENGTH_SUFFIX;
            if (size == 0 && !values.contains(length)
                    && !JsonText.arrayEntries(node.definition().defaultValue(), path).isEmpty()) {
                encoded.put(length, "0");
                visited.add(length);
            }
        }

        @Override
        void scalar(final PropertyNode node, final String path) {
            visited.add(path);
            if (!values.contains(path)) {
                return;
            }
            final String value = values.get(path);
            final boolean clearedText =
                    node.kind() == PropertyKind.STRING && !node.definition().defaultValue().isEmpty();
            if (!value.isEmpty() || node.repeats() || clearedText) {
                encoded.put(path, value);
            }
        }

        private Map<String, String> result() {
            keepElementsAlive();
            values.asMap().forEach((key, value) -> {
                if (!visited.contains(key) && !isUiCompanion(key) && !isStaleLength(key, value)) {
                    encoded.put(key, value);
                }
            });
            return encoded;
        }

        /** An element whose every value was left out would vanish, and the elements after it with it. */
        private void keepElementsAlive() {
            for (final ObjectArray objectArray : objectArrays) {
                final PropertyNode element = objectArray.array().element().orElseThrow();
                for (int position = 0; position < objectArray.size(); position++) {
                    final String elementPath = element.concretePath(objectArray.path(), position);
                    if (!hasEncodedKeyBelow(elementPath)) {
                        marker(element, elementPath).ifPresent(entry -> encoded.put(entry.getKey(), entry.getValue()));
                    }
                }
            }
        }

        private boolean hasEncodedKeyBelow(final String elementPath) {
            return encoded.keySet()
                    .stream()
                    .anyMatch(key -> key.startsWith(elementPath + '.')
                            || key.startsWith(elementPath + '[') || key.equals(elementPath))
                    || values.asMap()
                            .keySet()
                            .stream()
                            .anyMatch(key -> !visited.contains(key)
                                    && (key.startsWith(elementPath + '.') || key.startsWith(elementPath + '[')));
        }

        /** A zero bound only means "emptied on purpose" while the list is empty. */
        private boolean isStaleLength(final String key, final String value) {
            return key.endsWith(LENGTH_SUFFIX) && "0".equals(value)
                    && values.count(key.substring(0, key.length() - LENGTH_SUFFIX.length())) > 0;
        }
    }
}
