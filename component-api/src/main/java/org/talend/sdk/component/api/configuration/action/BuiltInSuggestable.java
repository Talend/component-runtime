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
package org.talend.sdk.component.api.configuration.action;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.meta.Documentation;

@ActionRef(Object.class)
@Documentation("Mark the decorated field as supporting suggestions, i.e. dynamically get a list of valid values the user can use. "
        + "It is however different from `@Suggestable` by looking up the implementation in the current application "
        + "and not the services. Finally, it is important to note that it can do nothing in some environments too and "
        + "that there is no guarantee the specified action is supported.")
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface BuiltInSuggestable {

    /**
     * @return name of the environment action to use.
     */
    Name value();

    /**
     * @return name of the environment action to use if value is {@link Name#CUSTOM}.
     */
    String name() default "";

    // no parameter yet, this is intended since it would be too dependent on the environment for now.

    enum Name {
        CUSTOM,
        INCOMING_SCHEMA_ENTRY_NAMES,

        CURRENT_SCHEMA_ENTRY_NAMES // studio only
    }
}
