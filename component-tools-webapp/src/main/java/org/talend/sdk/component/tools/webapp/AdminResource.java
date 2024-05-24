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
package org.talend.sdk.component.tools.webapp;

import static java.util.Optional.ofNullable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.server.dao.ComponentFamilyDao;
import org.talend.sdk.component.server.service.ComponentManagerService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("tools/admin")
@ApplicationScoped
public class AdminResource {

    @Inject
    private ComponentFamilyDao componentFamilyDao;

    @Inject
    private ComponentManagerService service;

    @HEAD
    @Path("{familyId}")
    public void reload(@PathParam("familyId") final String familyId) {
        final ComponentFamilyMeta family = ofNullable(componentFamilyDao.findById(familyId))
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        service
                .manager()
                .findPlugin(family.getPlugin())
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND))
                .get(ContainerManager.Actions.class)
                .reload();

        log.info("Reloaded family {}", family.getName());
    }
}
