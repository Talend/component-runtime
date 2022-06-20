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

import java.math.BigDecimal;

import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

public class Decimal extends LogicalType {

    private static final String DECIMAL = "decimal";

    public Decimal() {
        super(DECIMAL);
    }

    @Override
    public void validate(final Schema schema) {
        super.validate(schema);
        // validate the type
        if (schema.getType() != Schema.Type.STRING) {
            throw new IllegalArgumentException("Logical type decimal must be backed by string");
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static class DecimalConversion extends Conversion<BigDecimal> {

        @Override
        public Class<BigDecimal> getConvertedType() {
            return BigDecimal.class;
        }

        @Override
        public Schema getRecommendedSchema() {
            throw new UnsupportedOperationException("No recommended schema for decimal (scale is required)");
        }

        @Override
        public String getLogicalTypeName() {
            return "decimal";
        }

        @Override
        public BigDecimal fromCharSequence(final CharSequence value, final Schema schema, final LogicalType type) {
            if (value == null) {
                return null;
            }
            // TODO make sure here
            return new BigDecimal(value.toString());
        }

        @Override
        public CharSequence toCharSequence(final BigDecimal value, final Schema schema, final LogicalType type) {
            // TODO correct here
            return value.toString();
        }
    }

}
