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
package org.talend.sdk.component.api.configuration;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Mark a configuration field (injected in a
 * {@link org.talend.sdk.component.api.input.Emitter} or {@link org.talend.sdk.component.api.processor.Processor}
 * method parameter or nested in one of these method parameters).
 */
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Option {

    /**
     * Mark parameter of streaming {@link org.talend.sdk.component.api.input.PartitionMapper}
     * where {@link org.talend.sdk.component.api.input.PartitionMapper#infinite()} == true and
     * {@link org.talend.sdk.component.api.input.PartitionMapper#stoppable()} == true
     * that it contains max duration in milliseconds between start read the value and end
     * expected type of the parameter is long or int or their boxed equivalents
     */
    String MAX_DURATION_PARAMETER = "maxDurationMs";

    /**
     * Mark parameter of streaming {@link org.talend.sdk.component.api.input.PartitionMapper}
     * where {@link org.talend.sdk.component.api.input.PartitionMapper#infinite()} == true and
     * {@link org.talend.sdk.component.api.input.PartitionMapper#stoppable()} == true
     * that it limits amount of records that connector can read.
     * expected type of the parameter is long or int or their boxed equivalents
     */
    String MAX_RECORDS_PARAMETER = "maxRecords";

    /**
     * The value of the option, if empty it will use the classname + field value (ex:
     * {@code com.company.ConfigModel.data}).
     * 
     * @return the value of the option.
     */
    String value() default "";
}
