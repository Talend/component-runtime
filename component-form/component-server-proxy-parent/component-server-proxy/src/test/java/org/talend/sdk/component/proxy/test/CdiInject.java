/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(CdiInject.Extension.class)
public @interface CdiInject {

    class Extension implements TestInstancePostProcessor {

        @Override
        public void postProcessTestInstance(Object o, ExtensionContext extensionContext) {
            final CDI<Object> current = CDI.current();
            final BeanManager beanManager = current.getBeanManager();
            final AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(o.getClass());
            final InjectionTarget injectionTarget = beanManager.createInjectionTarget(annotatedType);
            injectionTarget.inject(o, beanManager.createCreationalContext(null));
        }
    }
}
