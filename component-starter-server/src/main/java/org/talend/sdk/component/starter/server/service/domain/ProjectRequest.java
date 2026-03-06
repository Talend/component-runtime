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
package org.talend.sdk.component.starter.server.service.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProjectRequest {

    private final String buildType;

    private final BuildConfiguration buildConfiguration;

    private final String packageBase;

    private final Collection<String> facets;

    private final Collection<SourceConfiguration> sources;

    private final Collection<ProcessorConfiguration> processors;

    private final Collection<ReusableConfiguration> configurations;

    private final String family;

    private final String category;

    private final JsonObject openapi;

    @Getter
    @RequiredArgsConstructor
    public static class ReusableConfiguration {

        private final String id;

        private final String name;

        private final DataStructure structure;

        private final String type; // datastore, dataset for now but could be extended in the future
    }

    @Getter
    @RequiredArgsConstructor
    public static class SourceConfiguration {

        private final String name;

        private final String icon;

        private final boolean stream;

        private final DataStructure configuration;

        private final StructureConfiguration outputStructure;
    }

    @Getter
    @RequiredArgsConstructor
    public static class DataStructure {

        private final List<Entry> entries;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Entry {

        private final String name;

        private final String type;

        private final String reference;

        private final DataStructure nestedType;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ProcessorConfiguration {

        private final String name;

        private final String icon;

        private final DataStructure configuration;

        private final Map<String, StructureConfiguration> inputStructures;

        private final Map<String, StructureConfiguration> outputStructures;
    }

    @Getter
    @RequiredArgsConstructor
    public static class StructureConfiguration {

        private final DataStructure structure;

        private final boolean generic;
    }

    @Getter
    @RequiredArgsConstructor
    public static class BuildConfiguration {

        private final String name;

        private final String description;

        private final String packaging;

        private final String group;

        private final String artifact;

        private final String version;

        private final String javaVersion;
    }
}
