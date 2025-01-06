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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;

@Target(TYPE)
@Retention(RUNTIME)
@Documentation("Mark a connector as having a fixed schema. " +
        "Annotation's value must match the name of a DiscoverSchema or a DiscoverSchemaExtended annotation. " +
        "The related action will return the fixed schema for the specified flows.")
public @interface FixedSchema {

    String value() default "";

    /**
     * The flows to apply the fixed schema.
     * With an Input component, it will always concern the main flow. So you can omit it.
     * With an Output component, it will concern either concern the main flow and/or the reject output flows.
     *
     * @return the flows concerned by fixed schema. Default to none.
     */
    String[] flows() default {};

}
