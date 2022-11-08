/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.di.JobStateAware;
import org.talend.sdk.component.runtime.di.beam.LoopState;
import org.talend.sdk.component.runtime.output.Branches;

class QueueOutputTest {

    @Test
    void execution() {
        final QueueOutput output = new QueueOutput("test", "test", "test", null);
        output.setState(new JobStateAware.State());
        output
                .onNext(name -> Branches.DEFAULT_BRANCH.equals(name) ? Json.createObjectBuilder().add("id", 1).build()
                        : null, name -> value -> {
                            throw new UnsupportedOperationException();
                        });
        final LoopState loopState = LoopState.lookup(output.getStateId());
        loopState.end();
        final Record next = loopState.next();
        assertNotNull(next);
        assertEquals(1, next.getInt("id"));
        assertNull(loopState.next());
    }
}
