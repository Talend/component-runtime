/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link AfterVariable}
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterVariables {

    AfterVariable[] value();

    /**
     * Put annotation on {@link org.talend.sdk.component.api.input.Emitter},
     * {@link org.talend.sdk.component.api.input.PartitionMapper},
     * {@link org.talend.sdk.component.api.processor.Processor} to declare after variables
     */
    @Repeatable(AfterVariables.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface AfterVariable {

        /**
         * @return studio name for variable (like: NB_LINE)
         */
        String value();

        /**
         * @return description of the variable. If it's an empty the key value might be used
         */
        String description() default "";

        /**
         * @return type of variable
         */
        Class<?> type() default String.class;
    }
}
