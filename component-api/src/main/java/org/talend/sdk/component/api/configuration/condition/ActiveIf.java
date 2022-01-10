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
package org.talend.sdk.component.api.configuration.condition;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import org.talend.sdk.component.api.configuration.condition.meta.Condition;
import org.talend.sdk.component.api.meta.Documentation;

@Repeatable(ActiveIfs.class)
@Documentation("If the evaluation of the element at the location matches value then the element is considered active, "
        + "otherwise it is deactivated.")
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Condition("if")
public @interface ActiveIf {

    /**
     * @return the path to evaluate.
     */
    String target();

    /**
     * @return the value to compare with the evaluated path.
     */
    String[] value();

    /**
     * Should the condition deduced from the target comparison to the value(s) be compared to true or false.
     * It is equivalent to see target and value defining a {@link java.util.function.Predicate} and this toggle
     * calling {@link Predicate#negate()}.
     *
     * @return if set to true it will be compared to false (reversed), otherwise it is compared to true.
     */
    boolean negate() default false;

    /**
     * @return the strategy to use to evaluate the value compared to value array.
     */
    EvaluationStrategy evaluationStrategy() default EvaluationStrategy.DEFAULT;

    EvaluationStrategyOption[] evaluationStrategyOptions() default {};

    /**
     * Allows to pass custom options to the evaluation strategy.
     * The supported ones are:
     * <ul>
     * <li>
     * For <code>CONTAINS</code> strategy:
     * <ul>
     * <li>lowercase: [true|false]</li>
     * </ul>
     * </li>
     * </ul>
     */
    @Target({})
    @Retention(RUNTIME)
    @interface EvaluationStrategyOption {

        /**
         * @return option name;
         */
        String name();

        /**
         * @return option value.
         */
        String value() default "true";
    }

    enum EvaluationStrategy {
        /**
         * Use the raw value.
         */
        DEFAULT,

        /**
         * For an array or string, evaluate the size of the value instead of the value itself.
         */
        LENGTH,

        /**
         * Check if a string or list of string contains a value.
         */
        CONTAINS
    }
}
