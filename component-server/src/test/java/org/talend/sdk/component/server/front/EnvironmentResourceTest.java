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
package org.talend.sdk.component.server.front;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.server.front.model.Environment;

@RunWith(MonoMeecrowave.Runner.class)
public class EnvironmentResourceTest {

    @Inject
    private WebTarget base;

    @Test
    public void environment() {
        final Environment environment = base.path("environment").request(APPLICATION_JSON_TYPE).get(Environment.class);
        assertEquals(1, environment.getLatestApiVersion());
        Stream.of(environment.getCommit(), environment.getTime(), environment.getVersion()).forEach(
                Assert::assertNotNull);
    }
}
