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
package org.talend.sdk.component.runtime.manager.reflect.visibility;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

class VisibilityServiceTest {

    private final VisibilityService service = new VisibilityService();

    @Test
    void one() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::if::target", "the_target");
        metadata.put("tcomp::condition::if::value", "a,b,c");
        metadata.put("tcomp::condition::if::negate", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy", "DEFAULT");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "foo", "foo", null, emptyList(),
                        emptyList(), metadata, false));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("the_target", "a").build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("the_target", "c").build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().build()));
    }

    @Test
    void two() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::if::target::0", "the_target1");
        metadata.put("tcomp::condition::if::value::0", "a,b,c");
        metadata.put("tcomp::condition::if::negate::0", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
        metadata.put("tcomp::condition::if::target::1", "the_target2");
        metadata.put("tcomp::condition::if::value::1", "1");
        metadata.put("tcomp::condition::if::negate::1", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::1", "LENGTH");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "foo", "foo", null, emptyList(),
                        emptyList(), metadata, false));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("the_target1", "a").build()));
        assertTrue(conditionGroup
                .isVisible(Json.createObjectBuilder().add("the_target1", "a").add("the_target2", "aa").build()));
        assertFalse(conditionGroup
                .isVisible(Json.createObjectBuilder().add("the_target1", "d").add("the_target2", "a").build()));
        assertFalse(conditionGroup
                .isVisible(Json.createObjectBuilder().add("the_target1", "d").add("the_target2", "aa").build()));
    }
}
