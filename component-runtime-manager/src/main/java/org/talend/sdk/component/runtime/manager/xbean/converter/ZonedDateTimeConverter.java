/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.xbean.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.xbean.propertyeditor.AbstractConverter;

public class ZonedDateTimeConverter extends AbstractConverter {

    private static final LocalTime NO_TIME = LocalTime.of(0, 0);

    private static final LocalDate NO_DATE = LocalDate.of(1970, 1, 1);

    private static final ZoneId UTC = ZoneId.of("UTC");

    public ZonedDateTimeConverter() {
        super(ZonedDateTime.class);
    }

    @Override
    protected Object toObjectImpl(final String data) {
        if (data.isEmpty()) {
            return null;
        }
        String text = data.replace('/', '-'); // sanitize date format
        switch (text.length()) {
        case 10: // YYYY-MM-dd
            return ZonedDateTime.of(LocalDate.parse(text), NO_TIME, ZoneId.of("UTC"));
        case 18: // HH:mm:ss.SSSSSSSSS
        case 15: // HH:mm:ss.SSSSSS
        case 12: // HH:mm:ss.SSS
        case 8: // HH:mm:ss
        case 5: // HH:mm
            return ZonedDateTime.of(NO_DATE, LocalTime.parse(text), UTC);
        case 29: // YYYY-MM-dd HH:mm:ss.SSSSSSSSS
        case 26: // YYYY-MM-dd HH:mm:ss.SSSSSS
        case 23: // YYYY-MM-dd HH:mm:ss.SSS
        case 19: // YYYY-MM-dd HH:mm:ss
        case 16: // YYYY-MM-dd HH:mm
        default: // YYYY-MM-dd HH:mm.ss+HH:mm[...]
            text = text.replace(' ', 'T');
            if (text.contains("+") || text.contains("[")) {
                int keepAsIt = text.indexOf('+');
                if (keepAsIt < 0) {
                    keepAsIt = text.indexOf('[');
                }
                // ensure we don't loose / in the []
                text = text.substring(0, keepAsIt) + data.substring(keepAsIt);
                return ZonedDateTime.parse(text);
            }
            final LocalDateTime dateTime =
                    LocalDateTime.parse(text.endsWith("Z") ? text.substring(0, text.length() - 1) : text);
            return ZonedDateTime.of(dateTime.toLocalDate(), dateTime.toLocalTime(), UTC);
        }
    }
}
