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

import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.configuration.ui.meta.Ui;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;

@Ui
@ActionRef(value = DiscoverSchema.class, ref = "discoverSchema")
@Documentation("Mark a List<String> or List<Object> field as being represented as the component data selector.")
@Retention(RUNTIME)
@Target({ PARAMETER, FIELD })
public @interface Structure {

    /**
     * @return the name of the input/output.
     */
    String value() default "__default__";

    /**
     * @return an optional discover schema reference.
     */
    String discoverSchema() default "";

    /**
     * @return true if current one support studio metadata dataset retrieve schema in UI
     */
    boolean supportMetadata() default false;

    /**
     * @return type of connection the field modelises.
     */
    Type type() default Type.IN;

    enum Type {
        IN,
        OUT
    }
}
