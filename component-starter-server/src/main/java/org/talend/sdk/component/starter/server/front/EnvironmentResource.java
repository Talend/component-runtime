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
package org.talend.sdk.component.starter.server.front;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.talend.sdk.component.starter.server.model.Environment;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("environment")
@ApplicationScoped
public class EnvironmentResource {

    @Inject
    private ServerInfo info;

    @GET
    public Environment info() {
        return new Environment(info.getLastUpdate(), info.getSnapshot().getKit(), info.getBranch(),
                info.getCommit(), info.getBuildTime(), info.getRelease());
    }
}
