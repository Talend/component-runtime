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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Build {
    private final String artifact;
    private final String mainJavaDirectory;
    private final String testJavaDirectory;
    private final String mainResourcesDirectory;
    private final String testResourcesDirectory;
    private final String mainWebResourcesDirectory;
    private final String buildFileName;
    private final String buildFileContent;
    private final String buildDir;
}
