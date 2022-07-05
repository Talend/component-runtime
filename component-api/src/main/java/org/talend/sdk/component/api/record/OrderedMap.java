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
package org.talend.sdk.component.api.record;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * On TCK Record, we have to control order of elements with keeping efficient access.
 * LinkedHashMap has efficient access, but order of element is only the order of insertions.
 * List (ArrayList, LinkedList) allow to fine control order but have inefficent access.
 * This class aims to control element order with keeping efficient access.
 * 
 * @param <T> : type of element.
 */
public class OrderedMap<T> {

    @AllArgsConstructor
    static class Node<T> implements Iterable<Node<T>> {

        @Getter
        public T value;

        public Node<T> next;

        public Node<T> prec;

        public Node<T> insert(final T newValue) {
            final Node<T> newNode = new Node<>(newValue, this.next, this);
            return this.insert(newNode);
        }

        public Node<T> insert(final Node<T> newNode) {
            if (newNode == this) {
                return newNode;
            }
            if (this.next != null) {
                this.next.prec = newNode;
            }
            newNode.prec = this;
            newNode.next = this.next;
            this.next = newNode;

            return newNode;
        }

        public void remove() {
            if (next != null) {
                this.next.prec = this.prec;
            }
            if (this.prec != null) {
                this.prec.next = this.next;
            }
            this.next = null;
            this.prec = null;
        }

        /**
         * switch current node with its next
         * For example : 1 <=> 2(this) <=> 3 <=> 4
         * will be transformed to : 1 <=> 3 <=> 2(this) <=> 4
         */
        public void swapWithNext() {
            final Node<T> nextNode = this.next;
            if (nextNode == null) {
                return;
            }
            // save what will be changed
            final Node<T> firstPrec = this.prec;
            final Node<T> nextNext = nextNode.next;

            // change next node
            nextNode.next = this;
            nextNode.prec = this.prec;

            // change prec.next to new next
            if (firstPrec != null) {
                firstPrec.next = nextNode;
            }
            this.prec = nextNode;
            this.next = nextNext;
            if (nextNext != null) {
                nextNext.prec = this;
            }
        }

        @Override
        public Iterator<Node<T>> iterator() {
            return new NodeIterator(this);
        }

        @AllArgsConstructor
        static class NodeIterator<T> implements Iterator<Node<T>> {

            private Node<T> current;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Node<T> next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException("no further node");
                }
                final Node<T> next = this.current;
                this.current = next.next;
                return next;
            }
        }
    }

    private Node<T> first;

    private Node<T> last;

    private final Map<String, Node<T>> values;

    private final Function<T, String> identifierGetter;

    public OrderedMap(final Function<T, String> identifierGetter) {
        this(identifierGetter, Collections.emptyList());
    }

    public OrderedMap(final Function<T, String> identifierGetter,
            final Iterable<T> inputValues) {
        this.values = new HashMap<>();
        this.identifierGetter = identifierGetter;
        inputValues.forEach(this::addValue);
    }

    public Stream<T> streams() {
        if (this.first == null) {
            return Stream.empty();
        }
        final Function<Node<T>, T> tFunction = (Node<T> f) -> f.getValue();
        return StreamSupport.stream(this.first.spliterator(), false)
                .map(tFunction);
    }

    public void forEachValue(final Consumer<T> valueConsumer) {
        if (this.first != null) {
            this.first.forEach((Node<T> node) -> valueConsumer.accept(node.getValue()));
        }
    }

    public void removeValue(final T value) {
        final String identifier = this.identifierGetter.apply(value);
        final Node<T> node = this.values.remove(identifier);
        if (node == null) {
            throw new IllegalArgumentException(
                    "No node '" + identifier + "' expected in values");
        }
        this.removeFromChain(node);
    }

    private void removeFromChain(final Node<T> node) {
        if (this.first == node) {
            this.first = node.next;
        }
        if (this.last == node) {
            this.last = node.prec;
        }
        node.remove();
    }

    public T getValue(final String identifier) {
        if (this.values != null) {
            return Optional.ofNullable(this.values.get(identifier)) //
                    .map(Node::getValue) //
                    .orElse(null);
        } else {
            return null;
        }
    }

    public void replace(final String identifier, final T newValue) {
        final Node<T> node = this.values.remove(identifier);
        if (node != null) {
            node.value = newValue;
            final String newIdentifier = this.identifierGetter.apply(newValue);
            this.values.put(newIdentifier, node);
        }
    }

    public void addValue(final T value) {
        final String name = this.identifierGetter.apply(value);
        if (this.values != null && this.values.containsKey(name)) {
            return;
        }
        if (this.first == null) {
            this.first = new Node<>(value, null, null);
            this.last = this.first;
            this.values.put(name, this.first);
        } else {
            final Node<T> newNode = this.last.insert(value);
            this.values.put(name, newNode);
            this.last = newNode;
        }
    }

    public void moveAfter(final String pivotIdentifier, final T valueToMove) {
        final Node<T> pivotNode = this.values.get(pivotIdentifier);
        if (pivotNode == null) {
            throw new IllegalArgumentException(String.format("%s not in schema", pivotIdentifier));
        }
        final String identifier = this.identifierGetter.apply(valueToMove);
        final Node<T> nodeToMove = this.values.get(identifier);

        this.removeFromChain(nodeToMove);
        pivotNode.insert(nodeToMove);
        if (pivotNode == this.last) {
            this.last = nodeToMove;
        }
    }

    /**
     * New Value should take place before 'pivotIdentifier' value
     *
     * @param pivotIdentifier : value pivotIdentifier to move before (pivot).
     * @param valueToMove : value to move.
     */
    public void moveBefore(final String pivotIdentifier, final T valueToMove) {
        final Node<T> nodePivot = this.values.get(pivotIdentifier);
        if (nodePivot == null) {
            throw new IllegalArgumentException(String.format("%s not in schema", pivotIdentifier));
        }
        final String newValueName = this.identifierGetter.apply(valueToMove);
        final Node<T> nodeToMove = this.values.get(newValueName);

        this.removeFromChain(nodeToMove);
        if (nodePivot == this.first) {
            nodeToMove.next = nodePivot;
            nodePivot.prec = nodeToMove;
            this.first = nodeToMove;
        } else {
            nodePivot.prec.insert(nodeToMove);
        }
    }

    public void swap(final String first, final String second) {
        if (first == null || second == null || first.equals(second)) {
            return;
        }
        final Node<T> firstNode = this.values.get(first);
        if (firstNode == null) {
            throw new IllegalArgumentException(String.format("%s not in schema", first));
        }
        final Node<T> secondNode = this.values.get(second);
        if (secondNode == null) {
            throw new IllegalArgumentException(String.format("%s not in schema", secondNode));
        }

        if (firstNode.next == secondNode) { // case first -> second direct
            firstNode.swapWithNext();
        } else if (secondNode.next == firstNode) { // case second -> first direct
            secondNode.swapWithNext();
        } else { // general case
                 // Put first node at second place
            final Node<T> firstPrec = firstNode.prec;
            final Node<T> firstNext = firstNode.next;

            final Node<T> secondPrec = secondNode.prec;
            final Node<T> secondNext = secondNode.next;

            firstNode.prec = secondPrec;
            if (secondPrec != null) {
                secondPrec.next = firstNode;
            }
            firstNode.next = secondNext;
            if (secondNext != null) {
                secondNext.prec = firstNode;
            }

            // Put second node at first place
            secondNode.prec = firstPrec;
            if (firstPrec != null) {
                firstPrec.next = secondNode;
            }
            secondNode.next = firstNext;
            if (firstNext != null) {
                firstNext.prec = secondNode;
            }
        }
        if (this.first == firstNode) {
            this.first = secondNode;
        } else if (this.first == secondNode) {
            this.first = firstNode;
        }
        if (this.last == firstNode) {
            this.last = secondNode;
        } else if (this.last == secondNode) {
            this.last = firstNode;
        }
    }

}
