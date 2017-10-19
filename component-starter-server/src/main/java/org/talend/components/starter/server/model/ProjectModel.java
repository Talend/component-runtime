// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.starter.server.model;

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
    // private String packaging; // jar only for now
    // private String javaVersion; // enforce to java 8 for now
    private Collection<String> facets;
    private Collection<Source> sources;
    private Collection<Processor> processors;

    @Getter
    @RequiredArgsConstructor
    public static class Source {

        private final String name;

        private final boolean genericOutput;

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
    }

    @Getter
    @RequiredArgsConstructor
    public static class Processor {

        private final String name;

        private final boolean genericInputs;

        private final boolean genericOutputs;

        private final Collection<String> inputs;

        private final Collection<String> outputs;

        private final Collection<Model> inputStructures;

        private final Collection<Model> outputStructures;
    }
}
