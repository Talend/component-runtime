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
package org.talend.sdk.component.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ComponentExceptionTest {

    @Test
    void testTypedCauseExceptionTransformation() {
        IndexOutOfBoundsException indexOutOfBoundsException = new IndexOutOfBoundsException("AAA");
        IllegalArgumentException illegalArgumentException =
                new IllegalArgumentException("BBB", indexOutOfBoundsException);
        IllegalStateException illegalStateException = new IllegalStateException("CCC", illegalArgumentException);

        ComponentException componentException = new ComponentException("DDD", illegalStateException);

        Throwable ccc = componentException.getCause();
        Assertions.assertEquals(Throwable.class, ccc.getClass());

        Throwable bbb = ccc.getCause();
        Assertions.assertEquals(Throwable.class, bbb.getClass());

        Throwable aaa = bbb.getCause();
        Assertions.assertEquals(Throwable.class, aaa.getClass());
    }

    @Test
    void testConstructor() {
        ComponentException componentException = new ComponentException("message");
        Assertions.assertNull(componentException.getCause());
    }

    @Test
    void testParameters() {
        ComponentException componentException = new ComponentException(new IndexOutOfBoundsException("cause"));
        Assertions.assertNotNull(componentException.getCause());
        Assertions.assertEquals("java.lang.IndexOutOfBoundsException", componentException.getOriginalType());
        Assertions.assertEquals(ComponentException.ErrorOrigin.UNKNOWN, componentException.getErrorOrigin());

        componentException = new ComponentException(ComponentException.ErrorOrigin.USER, "message");
        Assertions.assertEquals(ComponentException.ErrorOrigin.USER, componentException.getErrorOrigin());
    }

}