/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.chain;

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
