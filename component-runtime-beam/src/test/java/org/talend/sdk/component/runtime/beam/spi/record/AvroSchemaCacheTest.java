package org.talend.sdk.component.runtime.beam.spi.record;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class AvroSchemaCacheTest {

    private final AvroSchemaConverter converter = new AvroSchemaConverter();

    private final AvroSchemaCache cache = new AvroSchemaCache(converter::convert);

    @Test
    void find() {
        final List<SchemaImpl> schemas = this.provideSchemas();

        final List<Thread> threads =
                schemas.stream() //
                        .map((SchemaImpl s) -> new Thread(() -> this.treat(s))) //
                        .collect(Collectors.toList()); //
        threads.forEach(Thread::start);
        threads.forEach((Thread t) -> {
            try {
                t.join(30_000L);
            }
            catch (final InterruptedException ex)  {
                Thread.currentThread().interrupt();
            }
        });

        final SchemaImpl s7 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.BOOLEAN)
                        .withName("fieldBool")
                        .build())
                .build();
        this.treat(s7);
    }

    private void treat(final SchemaImpl schema) {
        final AvroSchema avroSchema = this.cache.find(schema);
        Assertions.assertNotNull(avroSchema);
        final AvroSchema ref = converter.convert(schema);
        Assertions.assertEquals(ref, avroSchema);

        Assertions.assertSame(ref, this.cache.find(ref));
    }

    private List<SchemaImpl> provideSchemas() {
        final SchemaImpl s1 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.STRING)
                        .withNullable(false)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World")
                .build();

        final SchemaImpl s2 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.STRING)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World")
                .build();

        final SchemaImpl s3 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.STRING)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World1")
                .build();

        final SchemaImpl s4 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.INT)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .build();

        final SchemaImpl s5 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.INT)
                        .withNullable(true)
                        .withName("fieldBis")
                        .build())
                .build();

        final SchemaImpl s6 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.FLOAT)
                        .withNullable(true)
                        .withName("fieldNext")
                        .build())
                .build();

        final SchemaImpl s2Bis = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new Schema.Entry.Builder()
                        .withType(Schema.Type.STRING)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World")
                .build();

        return Arrays.asList(s1, s2, s3, s4, s5, s6, s2Bis);
    }

}