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
package org.talend.sdk.component.api.service.schema;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;

@Target(METHOD)
@Retention(RUNTIME)
@ActionType(value = "schema_mapping", expectedReturnedType = String.class)
@Documentation("Mark a method as returning a database mapping from a connector configuration and some other parameters. "
        + "Use this annotation if database mapping can be dynamic and `@DatabaseMapping.Mapping` is set to `custom`. "
        + "The functionality is for the Studio only.")
public @interface DatabaseSchemaMapping {

    /**
     * @return the component family this action belongs to.
     */
    String family() default "";

    /**
     * @return the identifier usable by to reference this action.
     */
    String value() default "default";

}
