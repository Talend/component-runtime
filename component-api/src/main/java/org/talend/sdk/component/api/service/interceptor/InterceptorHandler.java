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
package org.talend.sdk.component.api.service.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The hook used to implement an interceptor.
 * It can be associated to a {@link Intercepts}
 * marker.
 *
 * IMPORTANT: if you want a delegate instance you have to provide a constructor
 * taking an object as parameter. Alternatively you can use a {@literal BiFunction<Method, Object[], Object>}
 * constructor parameter to be able to delegate the invocation. Else you must provide a default constructor.
 *
 * NOTE: if your interceptor doesn't take an invoker as parameter
 * ({@literal java.util.function.BiFunction<Method, Object[], Object>})
 * then the interceptor chain can be broken if you invoke yourself a method. It is recommended to respect it.
 */
public interface InterceptorHandler {

    /**
     * Called instead of the delegate method.
     *
     * @param method th emethod being invoked.
     * @param args the parameters of the method.
     * @return the returned value of the method.
     */
    Object invoke(Method method, Object[] args);

    default <T extends Annotation> Optional<T> findAnnotation(final Method method, final Class<T> type) {
        return Optional
                .ofNullable((T) Stream
                        .of(method.getAnnotations())
                        .filter(a -> a.annotationType() == type)
                        .findFirst()
                        .orElseGet(() -> Stream
                                .of(method.getDeclaringClass().getAnnotations())
                                .filter(a -> a.annotationType() == type)
                                .findFirst()
                                .orElse(null)));
    }
}
