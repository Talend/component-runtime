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
package org.talend.components.starter.server.service.domain;

import java.util.Collection;

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

    @Getter
    @RequiredArgsConstructor
    public static class SourceConfiguration {

        private final String name;

        private final boolean genericOutput;

        private final DataStructure outputStructure;
    }

    @Getter
    @RequiredArgsConstructor
    public static class DataStructure {

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
    public static class ProcessorConfiguration {

        private final String name;

        private final boolean genericInputs;

        private final boolean genericOutputs;

        private final Collection<String> inputs;

        private final Collection<String> outputs;

        private final Collection<DataStructure> inputStructures;

        private final Collection<DataStructure> outputStructures;
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
