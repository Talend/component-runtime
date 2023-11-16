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
package org.talend.sdk.component.server.front;

import java.util.Date;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.server.api.EnvironmentResource;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.Environment;
import org.talend.sdk.component.server.service.ComponentManagerService;

@ApplicationScoped
public class EnvironmentResourceImpl implements EnvironmentResource {

    private final AtomicReference<Environment> environment = new AtomicReference<>();

    @Inject
    @ConfigProperty(name = "git.build.version")
    private String version;

    @Inject
    @ConfigProperty(name = "git.commit.id")
    private String commit;

    @Inject
    @ConfigProperty(name = "git.build.time")
    private String time;

    @Inject
    private ComponentServerConfiguration configuration;

    @Inject
    private Instance<Application> applications;

    @Inject
    private ComponentManagerService service;

    private int latestApiVersion;

    private Date startTime;

    @PostConstruct
    private void init() {
        latestApiVersion = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(applications.iterator(), Spliterator.IMMUTABLE), false)
                .filter(a -> a.getClass().isAnnotationPresent(ApplicationPath.class))
                .map(a -> a.getClass().getAnnotation(ApplicationPath.class).value())
                .map(path -> path.replace("api/v", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1);
        startTime = new Date();
    }

    @Override
    public Environment get() {
        return new Environment(latestApiVersion, version, commit, time,
                configuration.getChangeLastUpdatedAtStartup() ? findLastUpdated() : service.findLastUpdated(),
                service.getConnectors());
    }

    private Date findLastUpdated() {
        final Date lastUpdated = service.findLastUpdated();
        return startTime.after(lastUpdated) ? startTime : lastUpdated;
    }
}
