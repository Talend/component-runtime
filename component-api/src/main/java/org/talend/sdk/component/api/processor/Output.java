/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.processor;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Output {

    /**
     * The output branch fed by this parameter.
     *
     * Used by {@link OutputEmitter} parameters, which always feed exactly one branch, and by
     * {@link MultiOutputIterator} parameters streaming to a single branch.
     *
     * @return the branch name, {@code __default__} if not set.
     */
    String value() default "__default__";

    /**
     * The branches a {@link MultiOutputIterator} parameter routes records to, when it feeds more than one.
     *
     * This is declarative metadata only: it lets the design/Studio layer know the output connections
     * of the component, the routing itself is done at runtime through {@link TaggedOutput} or
     * {@link MultiOutputIterator#setIterator(String, java.util.Iterator)}.
     * When left empty, {@link #value()} is used, which covers the single branch streaming case.
     * It is not supported on {@link OutputEmitter} parameters.
     *
     * @return the branch names, empty to fallback on {@link #value()}.
     */
    String[] branches() default {};
}
