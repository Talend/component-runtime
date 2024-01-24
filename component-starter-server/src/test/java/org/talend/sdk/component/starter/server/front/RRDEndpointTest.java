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
package org.talend.sdk.component.starter.server.front;

import static java.lang.Thread.sleep;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.talend.sdk.component.starter.server.model.ProjectModel;
import org.talend.sdk.component.starter.server.test.Client;

@MonoMeecrowaveConfig
@Client.Active
class RRDEndpointTest {

    @Test
    @Timeout(5)
    void metric(final WebTarget target) throws InterruptedException {
        ensureThereIsAProject(target);
        while (sumStats(target) <= 0) {
            sleep(250);
        }
    }

    private double sumStats(final WebTarget target) {
        final Map<String, Collection<RRDEndpoint.Point>> points =
                target.path("statistics/json").request(APPLICATION_JSON_TYPE).get(RRDEndpoint.Points.class).getPoints();
        return points.values().stream().flatMap(Collection::stream).mapToDouble(RRDEndpoint.Point::getValue).sum();
    }

    private void ensureThereIsAProject(final WebTarget target) {
        target
                .path("project/zip")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept("application/zip")
                .post(Entity.entity(new ProjectModel(), MediaType.APPLICATION_JSON_TYPE), byte[].class);
    }
}
