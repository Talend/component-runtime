/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.starter.server.model;

import java.util.Collection;

import lombok.Data;

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

    @Data
    public static class Source {

        private String name;

        private String icon;

        private boolean stream;

        private boolean genericOutput;

        private Model configurationStructure;

        private Model outputStructure;
    }

    @Data
    public static class Model {

        private Collection<Entry> entries;
    }

    @Data
    public static class Entry {

        private String name;

        private String type;

        private Model model;

        // todo: support @ObjectMap.Any, list etc...
    }

    @Data
    public static class Processor {

        private String name;

        private String icon;

        private Model configurationStructure;

        private Collection<NamedModel> inputStructures;

        private Collection<NamedModel> outputStructures;
    }

    @Data
    public static class NamedModel {

        private String name;

        private boolean generic;

        private Model structure;
    }
}
