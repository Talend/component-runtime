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
package org.talend.sdk.component.runtime.manager.xbean.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ZonedDateTimeConverterTest {

    private final ZonedDateTimeConverter converter = new ZonedDateTimeConverter();

    @CsvSource({ "2019/06/04 01:01:01,2019,06,04,01,01,01,utc", // localDateTime
            "2019/06/04,2019,06,04,-1,-1,-1,-1", // localDate
            "2019/06/04 01:01:01,2019,06,04,01,01,01,utc" // zonedDateTime
    })
    @ParameterizedTest
    void fromWebSubmit(final String date, final int year, final int month, final int day, final int hour,
            final int minute, final int second, final String zone) {
        final ZonedDateTime zonedDateTime = ZonedDateTime.class.cast(converter.toObjectImpl(date));
        if (year > 0) {
            assertEquals(year, zonedDateTime.getYear());
        }
        if (month > 0) {
            assertEquals(month, zonedDateTime.getMonthValue());
        }
        if (day > 0) {
            assertEquals(day, zonedDateTime.getDayOfMonth());
        }
        if (hour > 0) {
            assertEquals(hour, zonedDateTime.getHour());
        }
        if (minute > 0) {
            assertEquals(minute, zonedDateTime.getMinute());
        }
        if (second > 0) {
            assertEquals(second, zonedDateTime.getSecond());
        }
        if (!zone.startsWith("-")) {
            assertEquals(zone, zonedDateTime.getZone().getId().toLowerCase(Locale.ROOT));
        }
    }

    @Test
    void timeStringToObject() {
        final LocalTime now = LocalTime.now();
        converter.setAsText(now.toString());
        final Object converted = ZonedDateTime.class.cast(converter.getValue()).toLocalTime();
        assertEquals(now, converted);
        assertNotSame(now, converted);
    }

    @Test
    void timeObjectToString() {
        final LocalTime now = LocalTime.now();
        converter.setValue(now);
        assertEquals(now.toString(), converter.getAsText());
    }

    @Test
    void dateStringToObject() {
        final LocalDate now = LocalDate.now();
        converter.setAsText(now.toString());
        final Object converted = ZonedDateTime.class.cast(converter.getValue()).toLocalDate();
        assertEquals(now, converted);
        assertNotSame(now, converted);
    }

    @Test
    void dateObjectToString() {
        final LocalDate now = LocalDate.now();
        converter.setValue(now);
        assertEquals(now.toString(), converter.getAsText());
    }

    @Test
    void zonedDateTimeStringToObject() {
        final ZonedDateTime now = ZonedDateTime.now();
        converter.setAsText(now.toString());
        final Object converted = converter.getValue();
        assertEquals(now, converted);
        assertNotSame(now, converted);
    }

    @Test
    void zonedDateTimeObjectToString() {
        final ZonedDateTime now = ZonedDateTime.now();
        converter.setValue(now);
        assertEquals(now.toString(), converter.getAsText());
    }

    @Test
    void zonedDateTimeStringToObjectNoOffset() {
        final ZonedDateTime now = ZonedDateTime.now();
        converter.setAsText(now.toString().replaceFirst("\\[.+\\]", ""));
        final ZonedDateTime converted = ZonedDateTime.class.cast(converter.getValue());
        assertEquals(now.toLocalDate(), converted.toLocalDate());
        assertEquals(now.toLocalDateTime(), converted.toLocalDateTime());
    }

    @Test
    void zonedDateTimeStringToObjectUTC() {
        final ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"));
        Stream.of(now.toString(), now.toString().replaceFirst("\\[.+\\]", "")).forEach(text -> {
            converter.setAsText(text);
            final ZonedDateTime converted = ZonedDateTime.class.cast(converter.getValue());
            assertEquals(now.toLocalDate(), converted.toLocalDate());
            assertEquals(now.toLocalDateTime(), converted.toLocalDateTime());
        });
    }

    @Test
    void zonedDateTimeStringToObjectNoOffsetNoTimeZone() {
        final ZonedDateTime now = ZonedDateTime.now();
        converter.setAsText(now.toString().replaceFirst("\\[.+\\]", "").replaceFirst("\\+.+", ""));
        final ZonedDateTime converted = ZonedDateTime.class.cast(converter.getValue());
        assertEquals(now.toLocalDate(), converted.toLocalDate());
        assertEquals(now.toLocalDateTime(), converted.toLocalDateTime());
    }
}
