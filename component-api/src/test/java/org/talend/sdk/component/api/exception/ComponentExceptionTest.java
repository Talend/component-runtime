package org.talend.sdk.component.api.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentExceptionTest {

    @Test
    public void testTypedCauseExceptionTransformation(){
        IndexOutOfBoundsException indexOutOfBoundsException = new IndexOutOfBoundsException("AAA");
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("BBB", indexOutOfBoundsException);
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