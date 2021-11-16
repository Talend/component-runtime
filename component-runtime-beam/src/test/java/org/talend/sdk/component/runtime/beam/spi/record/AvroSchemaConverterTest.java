package org.talend.sdk.component.runtime.beam.spi.record;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class AvroSchemaConverterTest {

    @Test
    void convert() {
        AvroSchemaConverter converter = new AvroSchemaConverter();

        final SchemaImpl s1 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.STRING)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World")
                .build();
        final AvroSchema a1 = converter.convert(s1);
        Assertions.assertNotNull(a1);
        final org.apache.avro.Schema.Field field1 = a1.getDelegate().getField("field1");
        Assertions.assertNotNull(field1);
        Assertions.assertTrue(field1.schema().getTypes().stream().map(org.apache.avro.Schema::getType).anyMatch(org.apache.avro.Schema.Type.STRING::equals));
        Assertions.assertEquals("World", a1.getProp("Hello"));
    }
}