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
package org.talend.sdk.component.api.service.cache;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.talend.sdk.component.api.service.interceptor.InterceptorHandler;
import org.talend.sdk.component.api.service.interceptor.Intercepts;

/**
 * Can mark a method (or all methods) of a service as being cached in the {@link LocalCache}.
 */
@Intercepts(InterceptorHandler.class)
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Cached {

    /**
     * @return the cache ttl in milliseconds.
     */
    long timeout() default Integer.MAX_VALUE;
}
