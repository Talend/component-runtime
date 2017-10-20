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
package org.talend.components.runtime.manager.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.junit.Test;

public class ResolverImplTest {

    @Test
    public void resolvefromDescriptor() throws IOException {
        try (final InputStream stream = new ByteArrayInputStream(
                "The following files have been resolved:\njunit:junit:jar:4.12:compile".getBytes(StandardCharsets.UTF_8))) {
            final Collection<File> deps = new ResolverImpl(null, coord -> new File("maven2", coord))
                    .resolveFromDescriptor(stream);
            assertEquals(1, deps.size());
            String sep = File.separator;
            StringBuilder expectedPath = new StringBuilder().append("maven2").append(sep).append("junit").append(sep)
                    .append("junit").append(sep).append("4.12").append(sep).append("junit-4.12.jar");
            assertEquals(expectedPath.toString(), deps.iterator().next().getPath());
        }
    }
}
