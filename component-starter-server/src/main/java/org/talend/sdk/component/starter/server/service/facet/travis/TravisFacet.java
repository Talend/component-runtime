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
package org.talend.sdk.component.starter.server.service.facet.travis;

import java.util.Collection;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;

@ApplicationScoped
public class TravisFacet implements FacetGenerator {

    private InMemoryFile travisYml;

    public void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
        travisYml = new InMemoryFile(".travis.yml", "language: java\njdk:\n- oraclejdk8\nenv:\n" + "  global:\n"
                + "    - MAVEN_OPTS=\"-Dmaven.artifact.threads=64 -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn\"\n"
                + "cache:\n" + "  directories:\n  - \"$HOME/.m2\"\n"
                + "install: mvn clean install -DskipTests -Dinvoker.skip=true --batch-mode\n"
                + "script: mvn clean install -e --batch-mode\n");
    }

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors, final ServerInfo.Snapshot versions) {
        return Stream.of(travisYml);
    }

    @Override
    public String readme() {
        return "The .travis.yml file created at the root of the project is preconfigured to be able to build "
                + "a common component. It caches the maven repository to speed up builds and decrease the maven "
                + "log level to avoid to reach Travis CI output limit too fast for no reason.\n\n"
                + "More information can be found at "
                + "link:https://docs.travis-ci.com/user/getting-started/#To-get-started-with-Travis-CI[Get Started with Travis CI].";
    }

    @Override
    public String description() {
        return "Creates a .travis.yml pre-configured for a component build.";
    }

    @Override
    public String name() {
        return "Travis CI";
    }

    @Override
    public Category category() {
        return Category.TOOL;
    }
}
