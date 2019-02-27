/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.server;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.extension.stitch.server.ProcessCommandMapper;

/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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

@MonoMeecrowaveConfig
class ProcessCommandMapperTest {

    @Inject
    private ProcessCommandMapper mapper;

    @Test
    void mapped() {
        assertEquals(asList("foo", "bar"), mapper.toCommand(getClass().getSimpleName() + "_mapped", "thepath"));
    }

    @Test
    void placeholder() {
        final String tap = getClass().getSimpleName() + "_placeholder";
        assertEquals(asList("thepath", tap, "done"), mapper.toCommand(tap, "thepath"));
    }

    @Test
    void defaultCommand() {
        assertEquals(asList("thetap", "-c", "thepath"), mapper.toCommand("thetap", "thepath"));
    }
}
