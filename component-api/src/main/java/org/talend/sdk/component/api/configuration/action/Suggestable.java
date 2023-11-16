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
package org.talend.sdk.component.api.configuration.action;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.completion.Suggestions;

@ActionRef(Suggestions.class)
@Documentation("Mark the decorated field as supporting suggestions, i.e. dynamically get a list of valid values the user can use. "
        + "Note that if you are looking to provide proposals by form (depending the option(s) values), @Suggestable is a better fit.")
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Suggestable {

    /**
     * @return value of @{@link Suggestions} value method.
     */
    String value();

    /**
     * This "list" will represent the parameter the caller will send to the suggestions implementation.
     *
     * Syntax is the following:
     *
     * <ul>
     * <li>.: represents the decorated option (aka "this")</li>
     * <li>../foo: represents the
     *
     * <pre>
     * foo
     * </pre>
     *
     * option of the parent (if exists) of "."</li>
     * <li>bar: represents the
     *
     * <pre>
     * bar
     * </pre>
     *
     * sibling option of the decorated field</li>
     * <li>bar/dummy: represents the
     *
     * <pre>
     * dummy
     * </pre>
     *
     * option of the child bar of the decorated field</li>
     * </ul>
     *
     * This syntax is close to path syntax but the important point is all the parameters are related to the decorated
     * option.
     *
     * @return parameters for the validation.
     */
    String[] parameters() default { "." };
}
