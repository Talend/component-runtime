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
package org.talend.components.starter.server.service.facet;

import java.util.Collection;
import java.util.stream.Stream;

import org.talend.components.starter.server.service.domain.Build;
import org.talend.components.starter.server.service.domain.Dependency;
import org.talend.components.starter.server.service.domain.ProjectRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface FacetGenerator {

    String description();

    String name();

    Category category();

    default String readme() {
        return "";
    }

    default Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors) {
        return Stream.empty();
    }

    default Stream<Dependency> dependencies(final Collection<String> facets) {
        return Stream.empty();
    }

    @Getter
    @RequiredArgsConstructor
    class InMemoryFile {

        private final String path;

        private final String content;
    }

    enum Category {
        TEST("Test"),
        LIBRARIES("Libraries");

        @Getter
        private final String humanName;

        Category(final String humanName) {
            this.humanName = humanName;
        }
    }
}
