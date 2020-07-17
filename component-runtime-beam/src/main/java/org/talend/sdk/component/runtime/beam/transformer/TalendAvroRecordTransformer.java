package org.talend.sdk.component.runtime.beam.transformer;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.beam.spi.record.AvroSchema;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TalendAvroRecordTransformer {

    private RecordBuilderFactory recordBuilderFactory;

    private boolean sampleFailOnMismatchedTypes = true;

    private Schema unwrapFromFixedAvroSchema;

    public void setUnwrapFromFixedAvroSchema(Schema unwrapFromFixedAvroSchema) {
        this.unwrapFromFixedAvroSchema = unwrapFromFixedAvroSchema;
    }

    public void setSampleFailOnMismatchedTypes(boolean sampleFailOnMismatchedTypes) {
        this.sampleFailOnMismatchedTypes = sampleFailOnMismatchedTypes;
    }

    public TalendAvroRecordTransformer(RecordBuilderFactory recordBuilderFactory) {
        this.recordBuilderFactory = recordBuilderFactory;
    }

    /**
     * Transform current record to fixed avroschema based record
     * */
    public Record.Builder transformToAlignedRecordWithFixedAvroSchema(AvroSchema fixedAvroSchema, AvroRecord currentRecord) {
        Record.Builder aBuilder = recordBuilderFactory.newRecordBuilder();

        Map<String, Entry> fixedSchemaByColumnName = fixedAvroSchema.getEntries()
                .stream()
                .collect(Collectors
                        .toMap(v -> v.getName(), v -> v));
        for (Entry entry : currentRecord.getSchema().getEntries()) {
            Entry fixedColumnSchema = fixedSchemaByColumnName.get(entry.getName());
            if (fixedColumnSchema != null) {
                alignRecordWithFixedSchema(entry.getName(), fixedColumnSchema, currentRecord, aBuilder);
            }
        }
        return aBuilder;
    }

    /**
     * Create a new record from current record for matching the good value to good head based on fixed schema
     * */
    private void alignRecordWithFixedSchema(String columnName,
                                            Entry fixedColumnSchema,
                                            AvroRecord currentRecord,
                                            Record.Builder aBuilder) {
        try {
            switch (fixedColumnSchema.getType()) {
                case STRING:
                    currentRecord
                            .getOptionalString(columnName)
                            .ifPresent(v -> aBuilder.withString(columnName, v));
                    break;
                case LONG:
                    currentRecord
                            .getOptionalLong(columnName)
                            .ifPresent(v -> aBuilder.withLong(columnName, v));
                    break;
                case INT:
                    currentRecord
                            .getOptionalInt(columnName)
                            .ifPresent(v -> aBuilder.withInt(columnName, v));
                    break;
                case DOUBLE:
                    currentRecord
                            .getOptionalDouble(columnName)
                            .ifPresent(v -> aBuilder.withDouble(columnName, v));
                    break;
                case FLOAT:
                    currentRecord
                            .getOptionalFloat(columnName)
                            .ifPresent(v -> aBuilder.withFloat(columnName, (float) v));
                    break;
                case BOOLEAN:
                    currentRecord
                            .getOptionalBoolean(columnName)
                            .ifPresent(v -> aBuilder.withBoolean(columnName, v));
                    break;
                case BYTES:
                    currentRecord
                            .getOptionalBytes(columnName)
                            .ifPresent(v -> aBuilder.withBytes(columnName, v));
                    break;
                case DATETIME:
                    currentRecord
                            .getOptionalDateTime(columnName)
                            .ifPresent(v -> aBuilder.withDateTime(columnName, v));
                    break;
                case RECORD:
                    currentRecord
                            .getOptionalRecord(columnName)
                            .ifPresent(v -> aBuilder.withRecord(columnName, v));
                    break;
                case ARRAY:
                    currentRecord
                            .getOptionalArray(Object.class, columnName)
                            .ifPresent(v -> aBuilder.withArray(fixedColumnSchema, v));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported entry type: " + fixedColumnSchema.getType().toString());
            }
        } catch (IllegalArgumentException ex) {
            log.error("TalendAvroRecordTransformer can't transform this record : {}, due to {} and " +
                    "fixed column schema has type {}.", currentRecord, ex.getMessage(), fixedColumnSchema.getType());
            if (sampleFailOnMismatchedTypes) {
                throw new IllegalArgumentException("Column " + columnName + " of Record " + currentRecord.toString()
                        + " can't match fixed entry type: " + fixedColumnSchema.getType().toString());
            }
        }
    }

    /**
     * Check if two avro schema is exact the same avro schema
     * */
    public boolean checkIsSameAvroSchema(AvroSchema secondAvroSchema) {
        if (secondAvroSchema == null)
            return true;

        return unwrapFromFixedAvroSchema.equals(secondAvroSchema.unwrap(Schema.class));
    }
}
