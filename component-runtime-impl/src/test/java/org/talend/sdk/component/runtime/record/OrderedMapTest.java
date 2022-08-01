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
package org.talend.sdk.component.runtime.record;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.OrderedMap;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

class OrderedMapTest {

    @Test
    void t1() {
        RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test");
        OrderedMap<Schema.Entry> container = new OrderedMap<>(Schema.Entry::getName, Collections.emptyList());

        Schema.Entry f1 = factory.newEntryBuilder().withName("f1").withType(Schema.Type.STRING).build();
        Schema.Entry f2 = factory.newEntryBuilder().withName("f2").withType(Schema.Type.STRING).build();
        Schema.Entry f3 = factory.newEntryBuilder().withName("f3").withType(Schema.Type.STRING).build();
        Schema.Entry f4 = factory.newEntryBuilder().withName("f4").withType(Schema.Type.STRING).build();
        Schema.Entry f5 = factory.newEntryBuilder().withName("f5").withType(Schema.Type.STRING).build();

        container.addValue(f1);
        container.addValue(f2);
        List<Schema.Entry> entries = container.streams().collect(Collectors.toList());
        Assertions.assertEquals("f1", entries.get(0).getName());
        Assertions.assertEquals("f2", entries.get(1).getName());

        container.swap("f1", "f2");
        List<Schema.Entry> entries2 = container.streams().collect(Collectors.toList());
        Assertions.assertEquals("f2", entries2.get(0).getName());
        Assertions.assertEquals("f1", entries2.get(1).getName());

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

        Schema.Entry f1Bis = factory.newEntryBuilder().withName("f1Bis").withType(Schema.Type.STRING).build();
        container.replace("f1", f1Bis);
        Assertions.assertEquals("f4,f3,f1Bis,f5,f2", this.reduce(container));
    }

    private String reduce(OrderedMap<Schema.Entry> container) {
        return container.streams().map(Schema.Entry::getName).collect(Collectors.joining(","));
    }

}