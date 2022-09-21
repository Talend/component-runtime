package org.talend.sdk.component.runtime.tdp;

import java.util.HashMap;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

public class TdpRecordBuilderFactory implements RecordBuilderFactory {
    @Override
    public Record.Builder newRecordBuilder(final Schema schema, final Record record) {
        return TdpRecord.builder(schema, record);
    }

    @Override
    public Record.Builder newRecordBuilder(final Schema schema) {
        return TdpRecord.builder(schema);
    }

    @Override
    public Record.Builder newRecordBuilder() {
        return new TdpRecord.Builder();
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Schema.Type type) {
        return TdpSchema.builder()
                .withType(type);
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Schema schema) {
        Schema.Builder builder = TdpSchema.builder()
                .withType(schema.getType())
                .withProps(new HashMap<>(schema.getProps()));

        schema.getEntries().forEach(builder::withEntry);

        return builder;
    }

    @Override
    public Schema.Entry.Builder newEntryBuilder() {
        return TdpEntry.builder();
    }

    @Override
    public Schema.Entry.Builder newEntryBuilder(final Schema.Entry entry) {
        return entry.toBuilder();
    }
}
