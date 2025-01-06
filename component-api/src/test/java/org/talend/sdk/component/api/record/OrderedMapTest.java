/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OrderedMapTest {

    @Test
    void orderedMapTest() {
        final OrderedMap<String> container = new OrderedMap<>(Function.identity(), Collections.emptyList());

        String f1 = "f1";
        String f2 = "f2";
        String f3 = "f3";
        String f4 = "f4";
        String f5 = "f5";

        container.addValue(f1);
        container.addValue(f2);
        List<String> entries = container.streams().collect(Collectors.toList());
        Assertions.assertSame("f1", entries.get(0));
        Assertions.assertSame("f2", entries.get(1));

        container.swap("f1", "f2");
        List<String> entries2 = container.streams().collect(Collectors.toList());
        Assertions.assertSame("f2", entries2.get(0));
        Assertions.assertSame("f1", entries2.get(1));

        container.addValue(f3); // f2, f1, F3
        Assertions.assertEquals("f2,f1,f3", this.reduce(container));
        container.moveBefore("f2", f3); // F3, f2, F1
        Assertions.assertEquals("f3,f2,f1", this.reduce(container));

        container.addValue(f4);
        Assertions.assertEquals("f3,f2,f1,f4", this.reduce(container));

        container.moveBefore("f2", f4); // F3, f4, f2, F1
        Assertions.assertEquals("f3,f4,f2,f1", this.reduce(container));

        container.moveBefore("f2", f3);
        Assertions.assertEquals("f4,f3,f2,f1", this.reduce(container));

        container.addValue(f5);
        container.moveAfter("f2", f3);
        Assertions.assertEquals("f4,f2,f3,f1,f5", this.reduce(container));
        container.moveAfter("f5", f2);
        Assertions.assertEquals("f4,f3,f1,f5,f2", this.reduce(container));

        String f1Bis = "f1Bis";
        container.replace("f1", f1Bis);
        Assertions.assertEquals("f4,f3,f1Bis,f5,f2", this.reduce(container));
    }

    private String reduce(final OrderedMap<String> container) {
        return container.streams().collect(Collectors.joining(","));
    }

    @Test
    void forEachTest() {
        final OrderedMap<Integer> container = new OrderedMap<>(Integer::toUnsignedString, Collections.emptyList());
        Assertions.assertEquals(0l, container.streams().count());
        container.addValue(1);
        container.addValue(2);
        AtomicInteger number = new AtomicInteger(0);
        container.forEachValue(number::getAndAdd);
        Assertions.assertEquals(3, number.get());
    }

    @Test
    void orderedMapSwapTest() {
        final OrderedMap<String> container = new OrderedMap<>(Function.identity());

        String f1 = "f1";
        String f2 = "f2";
        String f3 = "f3";
        String f4 = "f4";
        String f5 = "f5";

        container.addValue(f1);
        container.addValue(f2);
        Assertions.assertEquals("f1,f2", this.reduce(container));

        container.swap("f1", "f2");
        Assertions.assertEquals("f2,f1", this.reduce(container));
        container.swap("f1", "f2");
        Assertions.assertEquals("f1,f2", this.reduce(container));

        container.addValue(f3);
        container.addValue(f4);
        container.addValue(f5);

        container.swap("f3", "f5");
        Assertions.assertEquals("f1,f2,f5,f4,f3", this.reduce(container));

        container.swap("f3", "f4");
        Assertions.assertEquals("f1,f2,f5,f3,f4", this.reduce(container));

        container.swap("f1", "f4");
        Assertions.assertEquals("f4,f2,f5,f3,f1", this.reduce(container));

        container.swap("f2", "f3");
        Assertions.assertEquals("f4,f3,f5,f2,f1", this.reduce(container));

        container.moveAfter("f3", "f2");
        Assertions.assertEquals("f4,f3,f2,f5,f1", this.reduce(container));

        container.moveBefore("f2", "f1");
        Assertions.assertEquals("f4,f3,f1,f2,f5", this.reduce(container));

        container.removeValue("f1");
        Assertions.assertEquals("f4,f3,f2,f5", this.reduce(container));

        Assertions.assertEquals("f3", container.getValue("f3"));
        Assertions.assertNull(container.getValue("unknown"));

        container.replace("f3", "f3New");
        Assertions.assertEquals("f4,f3New,f2,f5", this.reduce(container));
    }

    @Test
    void nodeSwapTest() {
        // test without next
        final OrderedMap.Node<String> node1 = new OrderedMap.Node<>("v1", null, null);
        node1.swapWithNext();

        final OrderedMap.Node<String> node2 = new OrderedMap.Node<>("v2", null, null);
        node1.insert(node2);
        Assertions.assertSame(node1.next, node2);
        Assertions.assertSame(node1, node2.prec);
        Assertions.assertNull(node2.next);
        Assertions.assertNull(node1.prec);

        node1.swapWithNext();
        Assertions.assertSame(node2.next, node1);
        Assertions.assertSame(node2, node1.prec);
        Assertions.assertNull(node1.next);
        Assertions.assertNull(node2.prec);

        node2.swapWithNext(); // back to normal.

        final OrderedMap.Node<String> node3 = new OrderedMap.Node<>("v3", null, null);
        node2.insert(node3);

        final OrderedMap.Node<String> node4 = new OrderedMap.Node<>("v4", null, null);
        node3.insert(node4);

        final Iterator<OrderedMap.Node<String>> iterator = node1.iterator();
        int index = 1;
        while (iterator.hasNext()) {
            OrderedMap.Node<String> next = iterator.next();
            Assertions.assertEquals("v" + index, next.getValue());
            index++;
        }

        node2.swapWithNext();
        final Iterator<OrderedMap.Node<String>> iteratorBis = node1.iterator();
        index = 1;
        while (iterator.hasNext()) {
            OrderedMap.Node<String> next = iterator.next();
            if (index == 1 || index == 4) {
                Assertions.assertEquals("v" + index, next.getValue());
            } else {
                Assertions.assertEquals("v" + (5 - index), next.getValue()); // 3 then 2
            }
            index++;
        }
        Assertions.assertThrows(NoSuchElementException.class, iterator::next);
    }

}