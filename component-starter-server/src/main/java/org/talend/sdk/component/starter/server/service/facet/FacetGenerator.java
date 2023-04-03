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
package org.talend.sdk.component.starter.server.service.facet;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Stream;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

public interface FacetGenerator {

    String description();

    String name();

    Category category();

    default String loggingScope() {
        return "";
    }

    default String readme() {
        return "";
    }

    default Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors, final ServerInfo.Snapshot versions) {
        return Stream.empty();
    }

    default Stream<Dependency> dependencies(final Collection<String> facets, final ServerInfo.Snapshot versions) {
        return Stream.empty();
    }

    @Getter
    @ToString
    @RequiredArgsConstructor
    class InMemoryFile {

        private final String path;

        private final byte[] content;

        public InMemoryFile(final String path, final String content) {
            this.path = path;
            this.content = content.replace("\r", "")/* avoid side effect of the OS */.getBytes(StandardCharsets.UTF_8);
        }
    }

    enum Category {

        TEST("Test"),
        RUNTIME("Runtime"),
        LIBRARIES("Libraries"),
        TOOL("Tool");

        @Getter
        private final String humanName;

        Category(final String humanName) {
            this.humanName = humanName;
        }
    }
}
