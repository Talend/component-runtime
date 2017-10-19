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
package org.talend.components.dependencies;

import java.util.stream.Stream;

public interface Resolver {

    /**
     * @param rootLoader the parent classloader of the plugins.
     * @param artifact the plugin jar/directory.
     * @return the list of resources representing the depencies (relative paths).
     */
    Stream<String> resolve(ClassLoader rootLoader, String artifact);
}
