/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
 * Mark a method as retruning the {@link Schema} of a dataset. The only configuration
 * parameter will be an {@link org.talend.sdk.component.api.configuration.Option} named "dataset".
 */
@Partial("See Schema description.")
@ActionType(value = "schema", expectedReturnedType = org.talend.sdk.component.api.record.Schema.class)
@Target(METHOD)
@Retention(RUNTIME)
@Documentation("Mark an action as returning a discovered schema. Its parameter MUST be a dataset. "
        + "Dataset is configuration type annotated with @DataSet. "
        + "If component has multiple datasets, then dataset used as action parameter "
        + "should have the same identifier as this @DiscoverSchema. ")
public @interface DiscoverSchema {

    /**
     * @return the component family this action belongs to.
     */
    String family() default "";

    /**
     * @return the identifier usable by {@link org.talend.sdk.component.api.configuration.ui.widget.Structure}
     * to reference this action.
     */
    String value() default "default";
}
