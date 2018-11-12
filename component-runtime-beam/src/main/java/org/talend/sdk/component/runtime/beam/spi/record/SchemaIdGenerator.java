/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE) // todo: don't keep it static, make it pluggable and Record based (not avro)
public class SchemaIdGenerator {

    public static String generateRecordName(final List<Schema.Field> fields) {
        final long fingerprint = fingerprint(fields);
        return "org.talend.sdk.component.schema.generated.Record_" + fields.size()
                + (fingerprint < 0 ? "_n_" + (-fingerprint) : ("_" + fingerprint));
    }

    private static long fingerprint(final List<Schema.Field> fields) {
        return SchemaNormalization
                .parsingFingerprint64(Schema
                        .createRecord(fields
                                .stream()
                                .map(it -> new Schema.Field(it.name(), it.schema(), it.doc(), it.defaultValue(),
                                        it.order()))
                                .collect(toList())));
    }
}
