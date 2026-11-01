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

/**
 * Walks the property tree along the concrete paths of a set of values: objects are entered, lists and maps are
 * expanded once per entry. Subclasses decide what a value is and how many entries a collection has.
 */
abstract class ValueWalker {

    /** The number of entries of the list or map at {@code path}. */
    abstract int size(PropertyNode collection, String path);

    /** The number of entries of a list or map in {@code values}: a map entry exists once its key or value does. */
    static int countEntries(final ConfigurationValues values, final PropertyNode collection, final String path) {
        return collection.kind() == PropertyKind.MAP ? values.count(path + ".key", path + ".value")
                : values.count(path);
    }

    /** A single value, including an element of a list of values and the key or value of a map entry. */
    void scalar(final PropertyNode node, final String path) {
        // nothing by default
    }

    /** Called with the size of every list or map, before its entries are walked. */
    void collection(final PropertyNode node, final String path, final int size) {
        // nothing by default
    }

    /** Return false to skip a node and everything below it. */
    boolean enter(final PropertyNode node, final String path) {
        return true;
    }

    final void walk(final PropertyNode node, final String path) {
        if (!enter(node, path)) {
            return;
        }
        switch (node.kind()) {
            case OBJECT:
                for (final PropertyNode child : node.children()) {
                    walk(child, child.concretePath(path, 0));
                }
                break;
            case MAP:
                walkMap(node, path);
                break;
            case ARRAY_OF_OBJECT:
            case ARRAY_OF_PRIMITIVE:
                walkArray(node, path);
                break;
            default:
                scalar(node, path);
                break;
        }
    }

    private void walkArray(final PropertyNode array, final String path) {
        final int size = size(array, path);
        collection(array, path, size);
        final PropertyNode element = array.element().orElseThrow();
        for (int position = 0; position < size; position++) {
            walk(element, element.concretePath(path, position));
        }
    }

    private void walkMap(final PropertyNode map, final String path) {
        final int size = size(map, path);
        collection(map, path, size);
        final PropertyNode key = map.child("key[]").orElseThrow();
        final PropertyNode value = map.child("value[]").orElseThrow();
        for (int position = 0; position < size; position++) {
            walk(key, key.concretePath(path, position));
            walk(value, value.concretePath(path, position));
        }
    }
}
