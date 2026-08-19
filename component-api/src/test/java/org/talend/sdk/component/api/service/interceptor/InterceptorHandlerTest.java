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
package org.talend.sdk.component.api.service.interceptor;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InterceptorHandlerTest {

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnnot {

        String xx() default "";
    }

    static class C1 {

        @MyAnnot(xx = "C1.m1")
        public void m1() {
        }
    }

    @MyAnnot(xx = "C2.m2")
    static class C2 {

        public void m2() {
        }
    }

    @Test
    void findAnnotation() throws NoSuchMethodException {
        final InterceptorHandler handler = new InterceptorHandler() {

            @Override
            public Object invoke(Method method, Object[] args) {
                return null;
            }
        };
        final Optional<MyAnnot> m1 = handler.findAnnotation(C1.class.getDeclaredMethod("m1"), MyAnnot.class);
        Assertions.assertNotNull(m1);
        Assertions.assertTrue(m1.isPresent());
        final MyAnnot annot = m1.get();
        Assertions.assertEquals("C1.m1", annot.xx());

        final Optional<MyAnnot> m2 = handler.findAnnotation(C2.class.getDeclaredMethod("m2"), MyAnnot.class);
        Assertions.assertNotNull(m2);
        Assertions.assertTrue(m2.isPresent());
        final MyAnnot annot2 = m2.get();
        Assertions.assertEquals("C2.m2", annot2.xx());
    }
}