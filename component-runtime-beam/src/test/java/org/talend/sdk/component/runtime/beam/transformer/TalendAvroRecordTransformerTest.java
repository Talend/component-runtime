package org.talend.sdk.component.runtime.beam.transformer;

import org.junit.Assert;
import org.junit.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecord;
import org.talend.sdk.component.runtime.beam.spi.record.AvroSchema;

public class TalendAvroRecordTransformerTest {

    private RecordBuilderFactory recordBuilderFactory = new AvroRecordBuilderFactoryProvider().apply("AvroTransformWrapper");

    private TalendAvroRecordTransformer talendAvroRecordTransformer = new TalendAvroRecordTransformer(recordBuilderFactory);

    private Record firstRecord;

    private Record secondRecord;

    private Record thirdRecord;

    private void prepareTestRecord() {
        firstRecord = recordBuilderFactory
                .newRecordBuilder()
                .withString("Compa_Ratio", "0.946")
                .withString("Supervisory_Organization_Name", "Field Sales - EMEA")
                .build();
        secondRecord = recordBuilderFactory
                .newRecordBuilder()
                .withString("Rating_String", "3 - Meets Expectations")
                .withString("Compa_Ratio", "1.079")
                .withString("Supervisory_Organization_Name", "Field Sales - Emerging Markets")
                .build();
        thirdRecord = recordBuilderFactory
                .newRecordBuilder()
                .withString("Rating_String", "GOGO")
                .withString("Compa_Ratio", "1.111")
                .withString("Supervisory_Organization_Name", "Field Sales - Emerging Markets")
                .build();
    }

    @Test
    public void test_TransformToAlignedRecordWithFixedAvroSchema() {
        prepareTestRecord();
        AvroSchema fixedSchema = (AvroSchema) firstRecord.getSchema();
        Record.Builder builder = talendAvroRecordTransformer.transformToAlignedRecordWithFixedAvroSchema(fixedSchema, (AvroRecord) secondRecord);
        Record transformedSecondRecord = builder.build();
        Assert.assertEquals(transformedSecondRecord.getString("Compa_Ratio"), "1.079");
        Assert.assertEquals(transformedSecondRecord.getString("Supervisory_Organization_Name"), "Field Sales - Emerging Markets");
    }

    @Test
    public void test_CheckDifferentAvroSchema() {
        prepareTestRecord();
        Schema unwrap = ((AvroSchema) firstRecord.getSchema()).unwrap(Schema.class);
        talendAvroRecordTransformer.setUnwrapFromFixedAvroSchema(unwrap);
        boolean isSameAvroSchema = talendAvroRecordTransformer.checkIsSameAvroSchema((AvroSchema) secondRecord.getSchema());
        Assert.assertFalse(isSameAvroSchema);
    }

    @Test
    public void test_CheckSameAvroSchema() {
        prepareTestRecord();
        Schema unwrap = ((AvroSchema) thirdRecord.getSchema()).unwrap(Schema.class);
        talendAvroRecordTransformer.setUnwrapFromFixedAvroSchema(unwrap);
        boolean isSameAvroSchemaBetweenSecondAndThirdRecord = talendAvroRecordTransformer.checkIsSameAvroSchema((AvroSchema) secondRecord.getSchema());
        Assert.assertTrue(isSameAvroSchemaBetweenSecondAndThirdRecord);
    }
}
