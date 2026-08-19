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
package org.talend.sdk.component.runtime.beam.customizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IndicesTest {

    @Test
    void sanityCheck() {
        assertTrue(Indices.BEAM_SDKS_JAVA_CORE.isAvailable());
        assertTrue(Indices.BEAM_SDKS_JAVA_CORE.getClasses().anyMatch("avro.shaded.com.google.common.base"::equals));
        assertFalse(Indices.BEAM_SDKS_JAVA_CORE.getClasses().anyMatch("org.talend.Foo"::equals));
    }
}
