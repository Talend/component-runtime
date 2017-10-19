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
package org.talend.components.test.dependencies;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;

public class DependenciesTxtBuilder {

    private final Collection<String> dependencies = new ArrayList<>();

    public DependenciesTxtBuilder withDependency(final String dep) {
        dependencies.add(dep);
        return this;
    }

    public String build() {
        return " \n The following files have been resolved:\n"
                + dependencies.stream().map(s -> "    " + s).collect(joining("\n"));
    }
}
