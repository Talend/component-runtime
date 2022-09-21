package org.talend.sdk.component.runtime.tdp;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TdpRecordsTest {

    public Schema.Entry newStringEntry(String entryName) {
        return TdpEntry.builder()
                .withName(entryName)
                .withType(Schema.Type.STRING)
                .build();
    }

    @Test
    public void shouldCreateRecordWithEntries() {
        // Given
        TdpRecordBuilderFactory recordBuilderFactory = new TdpRecordBuilderFactory();

        // When
        final Record record = recordBuilderFactory.newRecordBuilder()
                .withString("foo", "bar")
                .withString("baz", "boo")
                .build();

        // Then
        assertThat(record).isInstanceOf(TdpRecord.class);
        assertThat(record.getSchema()).isInstanceOf(TdpSchema.class);
        assertThat(record.getSchema().getEntries()).containsExactlyInAnyOrder(
                newStringEntry("foo"),
                newStringEntry("baz")
        );

        TdpRecord tdpRecord = (TdpRecord) record;
        assertThat(tdpRecord.getValues()).containsExactly(
                entry("foo", "bar"),
                entry("baz", "boo")
        );
    }

    @Test
    void shouldMutateRecordUnderTheHood() {
        // Given
        TdpRecordBuilderFactory recordBuilderFactory = new TdpRecordBuilderFactory();

        final Record existingRecord = recordBuilderFactory.newRecordBuilder()
                .withString("wouf", "waf")
                .build();

        // When
        final Record newRecord = existingRecord.toBuilder()
                .withString("miaou", "cat")
                .build();

        // Then
        assertThat(newRecord).isInstanceOf(TdpRecord.class);

        TdpRecord existingTdpRecord = (TdpRecord) existingRecord;
        TdpRecord newTdpRecord = (TdpRecord) newRecord;

        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("wouf", "waf");
        expectedValues.put("miaou", "cat");

        assertThat(newTdpRecord.getValues()).containsExactlyInAnyOrderEntriesOf(expectedValues);

        // Existing record has been mutated...
        assertThat(existingTdpRecord.getValues()).containsExactlyInAnyOrderEntriesOf(expectedValues);
    }
}
