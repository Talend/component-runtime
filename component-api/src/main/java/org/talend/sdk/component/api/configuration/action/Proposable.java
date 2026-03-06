/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.configuration.action;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.completion.DynamicValues;

@ActionRef(DynamicValues.class)
@Documentation("Mark the decorated field as supporting dynamic value filling (depending the server state and not known at component coding time). "
        + "Note that if you are looking to provide proposals by form (depending the option(s) values), @Suggestable is a better fit.")
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Proposable {

    /**
     * @return value of @{@link DynamicValues} value method.
     */
    String value();
}
