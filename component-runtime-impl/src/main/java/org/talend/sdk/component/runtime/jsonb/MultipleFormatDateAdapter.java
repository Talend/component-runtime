/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.jsonb;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import javax.json.bind.adapter.JsonbAdapter;

// will ensure we can parse the Record serialized format as well
public class MultipleFormatDateAdapter implements JsonbAdapter<Date, String> {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Override
    public Date adaptFromJson(final String obj) {
        try {
            return Date.from(LocalDateTime.parse(obj).toInstant(ZoneOffset.UTC));
        } catch (final DateTimeParseException pe) {
            return new Date(ZonedDateTime.parse(obj).toInstant().toEpochMilli());
        }
    }

    @Override
    public String adaptToJson(final Date obj) {
        return LocalDateTime.ofInstant(obj.toInstant(), UTC).toString();
    }
}
