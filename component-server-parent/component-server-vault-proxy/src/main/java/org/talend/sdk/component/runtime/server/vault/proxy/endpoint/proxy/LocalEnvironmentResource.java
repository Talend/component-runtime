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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.proxy;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Path("proxy")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class LocalEnvironmentResource {

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
    private Instance<Application> applications;

    private Environment environment;

    @PostConstruct
    private void init() {
        final int apiVersion = getApiVersion();
        environment = new Environment(apiVersion, apiVersion, version, commit, time);
    }

    private int getApiVersion() {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(applications.iterator(), Spliterator.IMMUTABLE), false)
                .filter(a -> a.getClass().isAnnotationPresent(ApplicationPath.class))
                .map(a -> a.getClass().getAnnotation(ApplicationPath.class).value())
                .map(path -> path.replace("api/v", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1);
    }

    @GET
    @Path("environment")
    public Environment get() {
        return environment;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Environment {

        private int latestApiVersion;

        private int proxiedApiVersion;

        private String version;

        private String commit;

        private String time;
    }
}
