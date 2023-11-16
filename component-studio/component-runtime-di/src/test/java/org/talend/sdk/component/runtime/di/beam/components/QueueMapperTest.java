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
package org.talend.sdk.component.runtime.di.beam.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.beam.LoopState;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;

class QueueMapperTest {

    @Test
    void split() {
        final QueueMapper mapper = new QueueMapper("test", "test", "test", null);
        try {
            mapper.setState(new JobStateAware.State());
        } catch (final NullPointerException npe) {
            // not sexy but for the test it is ok
        }
        final List<Mapper> split = mapper.split(mapper.assess());
        assertEquals(1, split.size());
        assertEquals(mapper, split.iterator().next());
    }

    @Test
    void execution() {
        final QueueMapper mapper = new QueueMapper("test", "test", "test", null);
        try {
            mapper.setState(new JobStateAware.State());
        } catch (final NullPointerException npe) {
            // not sexy but for the test it is ok
        }
        final Input input = mapper.create();
        final LoopState lookup = LoopState.lookup(mapper.getStateId());
        lookup.push(Json.createObjectBuilder().add("id", 1).build());
        lookup.end();
        assertEquals(1, Record.class.cast(input.next()).getInt("id"));
        assertNull(input.next());
    }
}
