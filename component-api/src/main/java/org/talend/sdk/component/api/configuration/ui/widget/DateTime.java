/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.configuration.ui.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.ui.meta.Ui;
import org.talend.sdk.component.api.meta.Documentation;

@Ui
@Documentation("Mark a field as being a date. "
        + "It supports and is *implicit* - which means you don't need to put that annotation on the option - "
        + "for `java.time.ZonedDateTime`, `java.time.LocalDate` and `java.time.LocalDateTime` "
        + "and is unspecified for other types.")
@Retention(RUNTIME)
@Target({ PARAMETER, FIELD })
public @interface DateTime {

    String dateFormat() default "YYYY/MM/DD";

    boolean useSeconds() default true;

    boolean useUTC() default true;
}
