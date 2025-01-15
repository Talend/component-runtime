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
import org.talend.sdk.component.api.meta.Partial;
import org.talend.sdk.component.api.service.ActionType;

/**
 * Mark a method as returning a {@link org.talend.sdk.component.api.record.Schema} resulting from an input one
 * and action of a processor for an outgoing branch.
 */
@Partial("See Schema description.")
@ActionType(value = "schema_extended", expectedReturnedType = org.talend.sdk.component.api.record.Schema.class)
@Target(METHOD)
@Retention(RUNTIME)
@Documentation("Mark a method as returning a Schema resulting from a connector configuration and some other parameters."
        +
        "Parameters can be an incoming schema and/or an outgoing branch." +
        "`value' name should match the connector's name.")
public @interface DiscoverSchemaExtended {

    /**
     * @return the component family this action belongs to.
     */
    String family() default "";

    /**
     * @return the identifier usable by to reference this action.
     */
    String value() default "default";
}