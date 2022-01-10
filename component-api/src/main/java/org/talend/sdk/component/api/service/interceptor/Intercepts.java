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
package org.talend.sdk.component.api.service.interceptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an annotation as being an interceptor marker.
 * See also {@link InterceptorHandler} too.
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface Intercepts {

    /**
     * The handler to use for that marker.
     * If not set an exception will be thrown.
     *
     * @return the handler class to use for that marker.
     */
    Class<? extends InterceptorHandler> value();

    /**
     * The interceptors are sorted by this number.
     *
     * @return the priority of the interceptor.
     */
    int priority() default 0;
}
