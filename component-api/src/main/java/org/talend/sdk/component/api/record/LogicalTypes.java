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
package org.talend.sdk.component.api.record;

public class LogicalTypes {

    private static final String UUID = "uuid";

    private static final String DATE = "date";

    private static final String TIME_NANOS = "time-nanos";

    private static final String TIMESTAMP_NANOS = "timestamp-nanos";

    private static final LogicalType UUID_TYPE = new LogicalType(UUID);

    private static final LogicalType DATE_TYPE = new LogicalType(DATE);

    private static final LogicalType TIME_NANOS_TYPE = new LogicalType(TIME_NANOS);

    private static final LogicalType TIMESTAMP_NANOS_TYPE = new LogicalType(TIMESTAMP_NANOS);

    public static LogicalType uuid() {
        return UUID_TYPE;
    }

    public static LogicalType date() {
        return DATE_TYPE;
    }

    public static LogicalType timeNanos() {
        return TIME_NANOS_TYPE;
    }

    public static LogicalType timestampNanos() {
        return TIMESTAMP_NANOS_TYPE;
    }

    public static LogicalType get(final String name) {
        if (name == null) {
            return null;
        }

        switch (name) {
        case UUID:
            return UUID_TYPE;
        case DATE:
            return DATE_TYPE;
        case TIME_NANOS:
            return TIME_NANOS_TYPE;
        case TIMESTAMP_NANOS:
            return TIMESTAMP_NANOS_TYPE;
        default:
            return null;
        }
    }

}
