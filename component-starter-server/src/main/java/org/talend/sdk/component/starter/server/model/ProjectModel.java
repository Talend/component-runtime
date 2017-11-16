/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.model;

import java.util.Collection;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public class ProjectModel {

    private String buildType;

    private String version;

    private String group;

    private String artifact;

    private String name;

    private String description;

    private String packageBase;

    private String family;

    private String category;

    // private String packaging; // jar only for now
    // private String javaVersion; // enforce to java 8 for now
    private Collection<String> facets;

    private Collection<Source> sources;

    private Collection<Processor> processors;

    @Getter
    @RequiredArgsConstructor
    public static class Source {

        private final String name;

        private final String icon;

        private final boolean stream;

        private final boolean genericOutput;

        private final Model configurationStructure;

        private final Model outputStructure;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Model {

        private final Collection<Entry> entries;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Entry {

        private final String name;

        private final String type;

        private final Model model;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Processor {

        private final String name;

        private final String icon;

        private final Model configurationStructure;

        private final Collection<NamedModel> inputStructures;

        private final Collection<NamedModel> outputStructures;
    }

    @Getter
    @RequiredArgsConstructor
    public static class NamedModel {

        private final String name;

        private final boolean generic;

        private final Model structure;
    }
}
