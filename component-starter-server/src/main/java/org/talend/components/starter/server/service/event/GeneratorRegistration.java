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
package org.talend.components.starter.server.service.event;

import java.util.HashMap;
import java.util.Map;

import org.talend.components.starter.server.service.build.BuildGenerator;
import org.talend.components.starter.server.service.facet.FacetGenerator;

import lombok.Getter;

@Getter
public class GeneratorRegistration {
    private final Map<String, BuildGenerator> buildGenerators = new HashMap<>();
    private final Map<String, FacetGenerator> facetGenerators = new HashMap<>();

    public GeneratorRegistration registerBuildType(final String build, final BuildGenerator generator) {
        buildGenerators.put(build, generator);
        return this;
    }

    public GeneratorRegistration registerFacetType(final FacetGenerator generator) {
        facetGenerators.put(generator.name(), generator);
        return this;
    }
}
