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
package org.talend.sdk.component.runtime.manager.chain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountingSuccessListenerTest {

    @Test
    public void counting() {
        final CountingSuccessListener listener = new CountingSuccessListener();
        assertEquals(0, listener.getCurrent());
        for (int i = 1; i < 10; i++) {
            listener.onData(new Object());
            assertEquals(i, listener.getCurrent());
        }
    }
}
