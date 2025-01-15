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
package org.talend.sdk.component.api.input;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface PartitionMapper {

    /**
     * @return the component of this mapper (family/grouping value).
     */
    String family() default "";

    /**
     * @return the value of the related input.
     */
    String name() default "";

    /**
     * If the @Producer method of the underlying {@link Emitter} can return null before the data
     * are completely read - i.e. infinite flow case - then you should set true to prevent the
     * execution to complete when null is encountered.
     *
     * Default cases matches a batch case whereas when set to true it matches a stream case.
     *
     * @return true if the underlying input can be used as a stream and not in batch context.
     */
    boolean infinite() default false;

    /**
     * Allow to provide an UI to customize a set of conditions to stop the infinite loop.
     * Only valid when {@code infinite()} returns true. So affects only streaming PartitionMappers.
     *
     * Caution:
     * Some records may be lost according the connector's Emitter implementation.
     * For instance, suppose that the connector maintains a queue with a size of 10 records, and we have in the stop
     * strategy a max records set to 5. If the first 10 records are considered as acknowledged, then the 5 first records
     * will be read but the 5 next will be considered lost.
     *
     * @return true if the underlying input can customize a stop strategy.
     */
    boolean stoppable() default false;

    /**
     * In Studio and some use-cases with some input connectors, we may need to ignore outgoing row.
     * As mentioned above, this is a Studio only feature.
     *
     * @return true if the underlying input is allowed to ignore mandatory output row.
     */
    boolean optionalRow() default false;
}
