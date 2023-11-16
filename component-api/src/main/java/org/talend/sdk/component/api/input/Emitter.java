/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Mark a class as returning an input connector.
 * This is only useful when there is no {@link PartitionMapper}.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Emitter {

    /**
     * @return the family of this mapper (family/grouping value).
     */
    String family() default "";

    /**
     * @return the value of the input.
     */
    String name() default "";

    /**
     * In Studio and some use-cases with some input connectors, we may need to ignore outgoing row.
     * As mentioned above, this is a Studio only feature.
     *
     * @return true if the underlying input is allowed to ignore mandatory output row.
     */
    boolean optionalRow() default false;
}
