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
package org.talend.sdk.component.api.service.asyncvalidation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;

@ActionType(value = "validation", expectedReturnedType = ValidationResult.class)
@Target(METHOD)
@Retention(RUNTIME)
@Documentation("Mark a method as being used to validate a configuration.\n\nIMPORTANT: this is a server validation "
        + "so only use it if you can't use other client side validation to implement it.")
public @interface AsyncValidation {

    /**
     * @return the value of the component family this action relates to.
     */
    String family() default "";

    /**
     * @return an identifier matched with
     * {@link org.talend.sdk.component.api.configuration.action.Validable}.
     */
    String value();
}
