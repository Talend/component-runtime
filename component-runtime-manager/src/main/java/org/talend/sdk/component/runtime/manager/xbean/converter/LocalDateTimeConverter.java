/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.apache.xbean.propertyeditor.AbstractConverter;

public class LocalDateTimeConverter extends AbstractConverter {

    public LocalDateTimeConverter() {
        super(LocalDateTime.class);
    }

    @Override
    protected Object toObjectImpl(final String text) {
        if (text.isEmpty()) {
            return null;
        }
        return ZonedDateTime.class.cast(new ZonedDateTimeConverter().toObjectImpl(text)).toLocalDateTime();
    }
}
