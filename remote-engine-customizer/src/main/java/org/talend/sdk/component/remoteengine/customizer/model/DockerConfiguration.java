/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.remoteengine.customizer.model;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.docker.CliDockerClient;

import org.talend.sdk.component.remoteengine.customizer.lang.IO;
import org.talend.sdk.component.remoteengine.customizer.lang.PathFactory;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;
import org.tomitribe.crest.api.Options;

@Options
public class DockerConfiguration {

    private final String path;

    private final String environment;

    public DockerConfiguration(@Option("path") final String path,
            @Option("environment") @Default("") final String environment) {
        this.path = path;
        this.environment = environment;
    }

    public DockerDaemonImage toImage(final String toConnectorsImage) throws InvalidImageReferenceException {
        final DockerDaemonImage docker = DockerDaemonImage.named(toConnectorsImage);
        environment().ifPresent(docker::setDockerEnvironment);
        ofNullable(path).ifPresent(p -> docker.setDockerExecutable(PathFactory.get(p)));
        return docker;
    }

    private Optional<Map<String, String>> environment() {
        return ofNullable(environment)
                .map(IO::loadProperties)
                .filter(p -> !p.isEmpty())
                .map(p -> p.stringPropertyNames().stream().collect(toMap(identity(), p::getProperty)));
    }

    public DockerClient toClient() {
        return new CliDockerClient(
                ofNullable(path).map(PathFactory::get)
                        .filter(Files::exists)
                        .orElse(CliDockerClient.DEFAULT_DOCKER_CLIENT),
                environment().orElseGet(Collections::emptyMap));
    }
}
