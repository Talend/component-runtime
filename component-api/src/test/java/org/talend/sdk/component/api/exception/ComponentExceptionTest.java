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
package org.talend.sdk.component.api.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentExceptionTest {

    @Test
    public void testTypedCauseExceptionTransformation() {
        IndexOutOfBoundsException indexOutOfBoundsException = new IndexOutOfBoundsException("AAA");
        IllegalArgumentException illegalArgumentException =
                new IllegalArgumentException("BBB", indexOutOfBoundsException);
        IllegalStateException illegalStateException = new IllegalStateException("CCC", illegalArgumentException);

        ComponentException componentException = new ComponentException("DDD", illegalStateException);

        Throwable ccc = componentException.getCause();
        Assertions.assertEquals(Exception.class, ccc.getClass());

        Throwable bbb = ccc.getCause();
        Assertions.assertEquals(Exception.class, bbb.getClass());

        Throwable aaa = bbb.getCause();
        Assertions.assertEquals(Exception.class, aaa.getClass());
    }

}