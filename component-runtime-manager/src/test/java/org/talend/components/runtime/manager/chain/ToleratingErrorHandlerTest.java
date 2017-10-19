// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager.chain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ToleratingErrorHandlerTest {

    @Test
    public void failAfterCount() {
        final ToleratingErrorHandler handler = new ToleratingErrorHandler(1);
        assertEquals(0, handler.getCurrent());
        handler.onError(new Object(), new RuntimeException());
        assertEquals(1, handler.getCurrent());
        try {
            handler.onError(new Object(), new RuntimeException());
            fail();
        } catch (final RuntimeException re) {
            assertEquals(2, handler.getCurrent());
        }
    }
}
