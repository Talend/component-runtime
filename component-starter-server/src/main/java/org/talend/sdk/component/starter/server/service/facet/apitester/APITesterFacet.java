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
package org.talend.sdk.component.starter.server.service.facet.apitester;

import java.util.Collection;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.info.ServerInfo.Snapshot;

@ApplicationScoped
public class APITesterFacet implements FacetGenerator {

    public static final String NAME = "APITesterFacet";

    public static final String REPOSITORIES = "   <repositories>\n" +
            "        <repository>\n" +
            "            <id>TalendOpenSourceRelease</id>\n" +
            "            <url>https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceRelease/</url>\n"
            +
            "            <releases>\n" +
            "                <enabled>true</enabled>\n" +
            "            </releases>\n" +
            "            <snapshots>\n" +
            "                <enabled>false</enabled>\n" +
            "            </snapshots>\n" +
            "        </repository>\n" +
            "        <repository>\n" +
            "            <id>talend.oss.snapshots</id>\n" +
            "            <url>https://artifacts-zl.talend.com/nexus/content/repositories/TalendOpenSourceSnapshot</url>\n"
            +
            "            <releases>\n" +
            "                <enabled>false</enabled>\n" +
            "            </releases>\n" +
            "            <snapshots>\n" +
            "                <enabled>true</enabled>\n" +
            "            </snapshots>\n" +
            "        </repository>\n" +
            "        <repository>\n" +
            "            <id>talend.releases</id>\n" +
            "            <url>https://artifacts-zl.talend.com/nexus/content/repositories/releases/</url>\n" +
            "            <releases>\n" +
            "                <enabled>true</enabled>\n" +
            "            </releases>\n" +
            "            <snapshots>\n" +
            "                <enabled>false</enabled>\n" +
            "            </snapshots>\n" +
            "        </repository>\n" +
            "    </repositories>";

    @Override
    public String description() {
        return "Talend API Tester";
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Category category() {
        return Category.LIBRARIES;
    }

    public String getRepositories() {
        return REPOSITORIES;
    }

    @Override
    public Stream<Dependency> dependencies(final Collection<String> facets, final Snapshot versions) {
        return Stream.of(
                new Dependency("org.junit.jupiter", "junit-jupiter", versions.getJupiter(), "test"),
                new Dependency("org.talend.sdk.component", "component-runtime-junit", versions.getKit(), "test"),
                new Dependency("org.talend.components", "stream-json", versions.getStreamJson(), "compile"),
                new Dependency("org.talend.ci", "api-tester-maven-plugin", versions.getApiTester(), "compile"));
    }

    public void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
    }

}
