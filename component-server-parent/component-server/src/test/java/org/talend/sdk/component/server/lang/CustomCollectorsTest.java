/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.lang;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.talend.sdk.component.server.lang.CustomCollectors.toLinkedMap;

import java.util.List;

import org.junit.jupiter.api.Test;

class CustomCollectorsTest {

    @Test
    void ensureOrderIsPreserved() {
        final List<String> expected = asList("a", "b", "c");
        assertIterableEquals(expected, expected.stream().collect(toLinkedMap(identity(), identity())).keySet());
    }
}
