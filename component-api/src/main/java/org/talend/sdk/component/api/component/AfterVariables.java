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
package org.talend.sdk.component.api.component;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.meta.Documentation;

/**
 * Use to group {@link AfterVariable} annotations.
 * Can be helpful either if you can't use {@link AfterVariable} as repeatable annotations or just to group annotations.
 *
 * Note. This functionality is for the Studio only.
 */
@Documentation("Declare the group of after variable `@AfterVariable`, "
        + "only supported on components - `@PartitionMapper`, `@Processor`, `@Emitter`."
        + "The functionality is for the Studio only.")
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface AfterVariables {

    AfterVariable[] value();

    /**
     * Declare after variable for the component.
     *
     * Put annotation on {@link org.talend.sdk.component.api.input.Emitter},
     * {@link org.talend.sdk.component.api.input.PartitionMapper},
     * {@link org.talend.sdk.component.api.processor.Processor} to declare after variables
     *
     * Note. This functionality is for the Studio only.
     */
    @Documentation("Declare the after variable, "
            + "only supported on components - `@PartitionMapper`, `@Processor`, `@Emitter`."
            + "The functionality is for the Studio only.")
    @Repeatable(AfterVariables.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Deprecated
    @interface AfterVariable {

        /**
         * @return studio name for variable (like: NB_LINE)
         */
        String value();

        /**
         * @return description of the variable. If it's an empty the key value might be used instead
         */
        String description() default "";

        /**
         * @return type of variable
         */
        Class<?> type() default String.class;
    }

    /**
     * Mark method that returns container with after variables.
     *
     * Note. This functionality is for the Studio only.
     */
    @Documentation("Mark method that returns container with after variables map, "
            + "only supported on components - `@PartitionMapper`, `@Processor`, `@Emitter`."
            + "The functionality is for the Studio only.")
    @Target(ElementType.METHOD)
    @Retention(RUNTIME)
    @Deprecated
    @interface AfterVariableContainer {
    }
}
