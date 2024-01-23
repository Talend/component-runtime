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
package org.talend.sdk.component.runtime.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(PER_CLASS)
class MultipleFormatDateAdapterTest {

    @Test
    void withJsonb() throws Exception {
        final ZonedDateTime zdtString = ZonedDateTime.ofInstant(new Date(0).toInstant(), ZoneId.of("UTC"));
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig())) {
            final DateHolder holder = jsonb.fromJson("{\"date\":\"" + zdtString + "\"}", DateHolder.class);
            assertEquals(new Date(0).getTime(), holder.date.getTime());
        }
    }

    public static class DateHolder {

        public Date date;
    }
}
