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

import org.talend.components.starter.server.service.facet.Versions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Dependency implements Versions {
    private static final Dependency JUNIT = new Dependency("junit", "junit", "4.12", "test");
    private static final Dependency COMPONENT = new Dependency("org.talend.components", "component-api", KIT, "provided");

    private final String group;
    private final String artifact;
    private final String version;
    private final String scope;
    private final String type;
    private final String classifier;

    public Dependency(final String group, final String artifact, final String version, final String scope) {
        this(group, artifact, version, scope, null, null);
    }

    public Dependency(final Dependency source, final String newScope) {
        this(source.getGroup(), source.getArtifact(), source.getVersion(), newScope, source.getType(), source.getClassifier());
    }

    public static Dependency componentApi() {
        return COMPONENT;
    }

    public static Dependency junit() {
        return JUNIT;
    }
}
