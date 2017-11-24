/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.avro;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.junit.Test;
import org.talend.sdk.component.runtime.avro.pojo.Aggregate;
import org.talend.sdk.component.runtime.avro.pojo.Complex;
import org.talend.sdk.component.runtime.avro.pojo.FallbackModel;
import org.talend.sdk.component.runtime.avro.pojo.ModelWithList;
import org.talend.sdk.component.runtime.avro.pojo.MyList;
import org.talend.sdk.component.runtime.avro.pojo.MyModel;
import org.talend.sdk.component.runtime.avro.pojo.MySelfReferencingModel;
import org.talend.sdk.component.runtime.avro.pojo.StringableModel;
import org.talend.sdk.component.runtime.manager.processor.SubclassesCache;

public class ComponentModelToIndexeredRecordConverterTest {

    private final ComponentModelToIndexeredRecordConverter converter = new ComponentModelToIndexeredRecordConverter();

    @Test
    public void mapPrimitiveToIR() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final IndexedRecord indexedRecord = converter.map(map);
        assertEquals(2, indexedRecord.getSchema().getFields().size());
        assertNotNull(indexedRecord.getSchema().getField("a"));
        assertNotNull(indexedRecord.getSchema().getField("b"));
        assertEquals(1, indexedRecord.get(indexedRecord.getSchema().getField("a").pos()));
        assertEquals(2, indexedRecord.get(indexedRecord.getSchema().getField("b").pos()));
    }

    @Test
    public void indexedRecordToPojoArray() {
        final GenericData.Record data = new GenericData.Record(
            SchemaBuilder.record("ComponentModelToIndexeredRecordConverterTest.indexedRecordToPojoArray.array").fields()
                .name("values").type().array().items().stringType().noDefault().endRecord());
        data.put(0, new GenericData.Array<>(data.getSchema().getField("values").schema(), asList("first", "second")));

        final MyList pojo = converter.reverseMapping(new SubclassesCache(), data, MyList.class);
        assertNotNull(pojo.getValues());
        assertEquals(2, pojo.getValues().size());
        assertEquals(asList("first", "second"), pojo.getValues());
    }

    @Test
    public void indexedRecordToPojoFlat() {
        final GenericData.Record data = new GenericData.Record(
            SchemaBuilder.record("ComponentModelToIndexeredRecordConverterTest.indexedRecordToPojoFlat.flat").fields()
                .name("name").type().stringType().noDefault().name("age").type().intType().noDefault()
                .name("optionalXp").type().nullable().intType().noDefault().endRecord());
        data.put(0, "test");
        data.put(1, 30);
        data.put(2, 300);

        final MyModel pojo = converter.reverseMapping(new SubclassesCache(), data, MyModel.class);
        assertNotNull(pojo);
        assertEquals("test", pojo.getName());
        assertEquals(30, pojo.getAge());
        assertEquals(300, pojo.getOptionalXp(), 0);

        data.put(2, null);
        assertNull(converter.reverseMapping(new SubclassesCache(), data, MyModel.class).getOptionalXp());
    }

    @Test
    public void indexedRecordToPojoNested() {
        final Schema aggregateSchema =
            SchemaBuilder.record("ComponentModelToIndexeredRecordConverterTest.indexedRecordToPojoNested.root").fields()
                .name("name").type().stringType().noDefault().name("nested").type()
                .record("ComponentModelToIndexeredRecordConverterTest.indexedRecordToPojoNested.nested").fields()
                .name("name").type().stringType().noDefault().name("age").type().intType().noDefault().endRecord()
                .noDefault().endRecord();

        final GenericData.Record nestedData = new GenericData.Record(aggregateSchema.getField("nested").schema());
        nestedData.put(0, "nest");
        nestedData.put(1, 30);

        final GenericData.Record data = new GenericData.Record(aggregateSchema);
        data.put(0, "test");
        data.put(1, nestedData);

        final Complex pojo = converter.reverseMapping(new SubclassesCache(), data, Complex.class);
        assertNotNull(pojo);
        assertEquals("test", pojo.getName());
        assertNotNull(pojo.getNested());
        assertEquals("nest", pojo.getNested().getName());
        assertEquals(30, pojo.getNested().getAge());
    }

    @Test
    public void nested() {
        final Complex.Nested nested = new Complex.Nested();
        nested.setAge(30);
        nested.setName("next");
        final Complex complex = new Complex();
        complex.setName("root");
        complex.setNested(nested);

        final IndexedRecord record = converter.map(complex);
        assertNotNull(record);
        assertEquals(complex.getName(), record.get(record.getSchema().getField("name").pos()));

        final IndexedRecord nestedRecord =
            IndexedRecord.class.cast(record.get(record.getSchema().getField("nested").pos()));
        assertEquals(complex.getNested().getAge(), nestedRecord.get(nestedRecord.getSchema().getField("age").pos()));
        assertEquals(complex.getNested().getName(), nestedRecord.get(nestedRecord.getSchema().getField("name").pos()));
    }

    @Test
    public void flatPojo() {
        final MyModel myModel = new MyModel();
        myModel.setAge(30);
        myModel.setOptionalXp(100);
        myModel.setName("Me");

        final IndexedRecord record = converter.map(myModel);
        assertNotNull(record);
        assertEquals(myModel.getAge(), record.get(record.getSchema().getField("age").pos()));
        assertEquals(myModel.getOptionalXp(), record.get(record.getSchema().getField("optionalXp").pos()));
        assertEquals(myModel.getName(), record.get(record.getSchema().getField("name").pos()));
    }

    @Test
    public void fallback() {
        final FallbackModel myModel = new FallbackModel();
        myModel.setData(singletonMap("a", 1));

        final IndexedRecord record = converter.map(myModel);
        assertNotNull(record);
        assertEquals(myModel.getData().get("a"), record.get(record.getSchema().getField("a").pos()));
    }

    @Test
    public void stringable() {
        final StringableModel myModel = new StringableModel();
        myModel.setValue(BigDecimal.valueOf(10));

        final IndexedRecord record = converter.map(myModel);
        assertNotNull(record);
        final Object value = record.get(record.getSchema().getField("value").pos());
        final BigDecimal converted =
            converter.reverseMapping(new SubclassesCache(), new GenericData.Record(record.getSchema()) {

                {
                    put(0, "10");
                }
            }, StringableModel.class).getValue();
        assertNotSame(value, converted);
        assertEquals(myModel.getValue(), value);
        assertEquals(myModel.getValue(), converted);
    }

    @Test
    public void list() {
        final ModelWithList root = new ModelWithList();
        root.setModels(new ArrayList<>());
        root.setStrings(asList("a1", "b2"));
        {
            final MyModel model = new MyModel();
            model.setName("first");
            model.setAge(30);
            root.getModels().add(model);
        }

        final IndexedRecord record = converter.map(root);
        assertNotNull(record);
        assertEquals(asList("a1", "b2"), record.get(record.getSchema().getField("strings").pos()));
        final GenericArray<IndexedRecord> models =
            GenericArray.class.cast(record.get(record.getSchema().getField("models").pos()));
        assertEquals(1, models.size());

        final IndexedRecord first = models.iterator().next();
        assertEquals(30, first.get(first.getSchema().getField("age").pos()));
        assertEquals("first", first.get(first.getSchema().getField("name").pos()));
        assertNull(first.get(first.getSchema().getField("optionalXp").pos()));
    }

    @Test
    public void referenceLoops() {
        final MySelfReferencingModel.Nested nested = new MySelfReferencingModel.Nested();
        final MySelfReferencingModel myModel = new MySelfReferencingModel();
        nested.setParent(myModel);
        myModel.setNested(nested);

        final IndexedRecord record = converter.map(myModel);
        assertNotNull(record);
        final IndexedRecord nestedIR =
            IndexedRecord.class.cast(record.get(record.getSchema().getField("nested").pos()));
        assertNotNull(nestedIR);
        final IndexedRecord parent =
            IndexedRecord.class.cast(nestedIR.get(nestedIR.getSchema().getField("parent").pos()));
        assertSame(parent, record);
    }

    @Test
    public void complexModel() {
        final Aggregate child = new Aggregate();
        child.setName("child");

        final Aggregate aggregate = new Aggregate();
        aggregate.setName("parent");
        aggregate.setWrapper(new Aggregate.ListWrapper());
        aggregate.getWrapper().setRecords(new ArrayList<>());
        aggregate.getWrapper().getRecords().add(child);

        final IndexedRecord record = converter.map(aggregate);
        assertNotNull(record);
        assertEquals("parent", record.get(record.getSchema().getField("name").pos()));
        final IndexedRecord wrapper =
            IndexedRecord.class.cast(record.get(record.getSchema().getField("wrapper").pos()));

        final GenericArray<IndexedRecord> list =
            GenericArray.class.cast(wrapper.get(wrapper.getSchema().getField("records").pos()));
        assertEquals(1, list.size());

        final IndexedRecord first = list.iterator().next();
        assertEquals("child", first.get(first.getSchema().getField("name").pos()));
        assertNull(first.get(first.getSchema().getField("wrapper").pos()));
    }

    @Test
    public void nullHandling() {
        final MyModel myModel = new MyModel();
        myModel.setAge(30);

        final IndexedRecord record = converter.map(myModel);
        assertNotNull(record);
        assertEquals(myModel.getAge(), record.get(record.getSchema().getField("age").pos()));
        assertEquals(myModel.getOptionalXp(), record.get(record.getSchema().getField("optionalXp").pos()));
        assertEquals(myModel.getName(), record.get(record.getSchema().getField("name").pos()));
    }
}
