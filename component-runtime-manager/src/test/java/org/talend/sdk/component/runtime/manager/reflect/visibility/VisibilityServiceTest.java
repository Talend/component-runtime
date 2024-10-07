/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect.visibility;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

class VisibilityServiceTest {

    private final VisibilityService service = new VisibilityService(JsonProvider.provider());

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
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("the_target", "a").build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("the_target", "c").build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().add("the_target", "d").build()));
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

    @Test
    void activeIfs() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::ifs::operator", "AND");
        metadata.put("tcomp::condition::if::target::0", "operator");
        metadata.put("tcomp::condition::if::value::0", "IS_NULL");
        metadata.put("tcomp::condition::if::negate::0", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
        metadata.put("tcomp::condition::if::target::1", "operator");
        metadata.put("tcomp::condition::if::value::1", "IS_NOT_NULL");
        metadata.put("tcomp::condition::if::negate::1", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::1", "DEFAULT");
        metadata.put("tcomp::condition::if::target::2", "operator");
        metadata.put("tcomp::condition::if::value::2", "IS_EMPTY");
        metadata.put("tcomp::condition::if::negate::2", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::2", "DEFAULT");
        metadata.put("tcomp::condition::if::target::3", "operator");
        metadata.put("tcomp::condition::if::value::3", "IS_NOT_EMPTY");
        metadata.put("tcomp::condition::if::negate::3", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::3", "DEFAULT");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "filter", "filter", null,
                        emptyList(), emptyList(), metadata, false));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("operator", "IS_NUMERIC").build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder().add("operator", "VALID").build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().add("operator", "IS_NULL").build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().add("operator", "IS_NOT_NULL").build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().add("operator", "IS_EMPTY").build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder().add("operator", "IS_NOT_EMPTY").build()));
    }

    @Test
    void activeIfsWithArray() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::ifs::operator", "AND");
        metadata.put("tcomp::condition::if::target::0", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::0", "IS_NULL");
        metadata.put("tcomp::condition::if::negate::0", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
        metadata.put("tcomp::condition::if::target::1", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::1", "IS_NOT_NULL");
        metadata.put("tcomp::condition::if::negate::1", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::1", "DEFAULT");
        metadata.put("tcomp::condition::if::target::2", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::2", "IS_EMPTY");
        metadata.put("tcomp::condition::if::negate::2", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::2", "DEFAULT");
        metadata.put("tcomp::condition::if::target::3", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::3", "IS_NOT_EMPTY");
        metadata.put("tcomp::condition::if::negate::3", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::3", "DEFAULT");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "filter", "filter", null,
                        emptyList(), emptyList(), metadata, false));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("operator", "IS_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_EMPTY").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_EMPTY").build()))
                .build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("operator", "IS_VALID").build()))
                .build()));
    }

    @Test
    void activeIfsWithArray_false() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::ifs::operator", "AND");
        metadata.put("tcomp::condition::if::target::0", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::0", "IS_NULL");
        metadata.put("tcomp::condition::if::negate::0", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
        metadata.put("tcomp::condition::if::target::1", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::1", "IS_NOT_NULL");
        metadata.put("tcomp::condition::if::negate::1", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy::1", "DEFAULT");
        metadata.put("tcomp::condition::if::target::2", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::2", "IS_EMPTY");
        metadata.put("tcomp::condition::if::negate::2", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy::2", "DEFAULT");
        metadata.put("tcomp::condition::if::target::3", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::3", "IS_NOT_EMPTY");
        metadata.put("tcomp::condition::if::negate::3", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy::3", "DEFAULT");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "filter", "filter", null,
                        emptyList(), emptyList(), metadata, false));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("operator", "IS_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_EMPTY").build()))
                .build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_EMPTY").build()))
                .build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("operator", "IS_VALID").build()))
                .build()));
    }
    @Test
    void activeIfWithArray() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::if::target::0", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::0", "IS_NULL,IS_NOT_NULL,IS_EMPTY,IS_NOT_EMPTY");
        metadata.put("tcomp::condition::if::negate::0", "true");
        metadata.put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "filter", "filter", null,
                        emptyList(), emptyList(), metadata, false));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("operator", "IS_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_EMPTY").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_EMPTY").build()))
                .build()));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("operator", "IS_VALID").build()))
                .build()));
    }

    @Test
    void activeIfWithArray_false() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("tcomp::condition::if::target::0", "arry[${index}].operator");
        metadata.put("tcomp::condition::if::value::0", "IS_NULL,IS_NOT_NULL,IS_EMPTY,IS_NOT_EMPTY");
        metadata.put("tcomp::condition::if::negate::0", "false");
        metadata.put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
        final VisibilityService.ConditionGroup conditionGroup = service
                .build(new ParameterMeta(null, String.class, ParameterMeta.Type.STRING, "filter", "filter", null,
                        emptyList(), emptyList(), metadata, false));
        assertTrue(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("operator", "IS_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_NULL").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_EMPTY").build())
                                .add(Json.createObjectBuilder().add("operator", "IS_NOT_EMPTY").build()))
                .build()));
        assertFalse(conditionGroup.isVisible(Json.createObjectBuilder()
                .add("arry", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("operator", "IS_VALID").build()))
                .build()));
    }
}
