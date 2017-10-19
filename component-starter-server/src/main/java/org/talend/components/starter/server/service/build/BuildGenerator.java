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
package org.talend.components.starter.server.service.build;

import java.util.Collection;

import org.talend.components.starter.server.service.domain.Build;
import org.talend.components.starter.server.service.domain.Dependency;
import org.talend.components.starter.server.service.domain.ProjectRequest;

public interface BuildGenerator {
    Build createBuild(ProjectRequest.BuildConfiguration buildConfiguration, String packageBase,
                      Collection<Dependency> dependencies, Collection<String> facets);
}
