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
package org.talend.sdk.component.runtime.tdp;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;

import static org.assertj.core.api.Assertions.assertThat;

public class TdpSchemaTest {

    @Test
    void testSchemaEquals() {
        final Schema referenceSchema = TdpSchema.builder()
                .withEntry(createStringEntry("foo"))
                .withEntry(createStringEntry("bar"))
                .withEntry(createStringEntry("baz"))
                .build();

        final Schema sameThanReferenceSchema = TdpSchema.builder()
                .withEntry(createStringEntry("foo"))
                .withEntry(createStringEntry("bar"))
                .withEntry(createStringEntry("baz"))
                .build();

        final Schema differentThanReferenceSchema = TdpSchema.builder()
                .withEntry(createStringEntry("foo"))
                .withEntry(createStringEntry("glou"))
                .withEntry(createStringEntry("baz"))
                .build();

        assertThat(sameThanReferenceSchema).isEqualTo(referenceSchema);
        assertThat(differentThanReferenceSchema).isNotEqualTo(referenceSchema);
    }

    private TdpEntry createStringEntry(String entryName) {
        return (TdpEntry) TdpEntry.builder()
                .withName(entryName)
                .withType(Schema.Type.STRING)
                .withRawName(entryName)
                .build();
    }
}
