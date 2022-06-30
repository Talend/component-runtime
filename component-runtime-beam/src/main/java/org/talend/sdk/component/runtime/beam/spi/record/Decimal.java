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
package org.talend.sdk.component.runtime.beam.spi.record;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

public class Decimal extends LogicalType {

    private static final String DECIMAL = "decimal";

    private static final Decimal DECIMAL_TYPE = new Decimal();

    private Decimal() {
        super(DECIMAL);
    }

    public static Decimal logicalType() {
        return DECIMAL_TYPE;
    }

    @Override
    public void validate(final Schema schema) {
        super.validate(schema);
        // validate the type
        if (schema.getType() != Schema.Type.STRING) {
            throw new IllegalArgumentException("Logical type decimal must be backed by string");
        }
    }

}
