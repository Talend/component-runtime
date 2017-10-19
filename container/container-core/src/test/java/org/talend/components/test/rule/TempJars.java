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
package org.talend.components.test.rule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.rules.ExternalResource;
import org.talend.components.test.Constants;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TempJars extends ExternalResource {

    private final Collection<File> toClean = new ArrayList<>();

    public File create(final String dependencies) {
        final File tmp = new File("target/tempsjars/" + UUID.randomUUID().toString() + ".jar");
        if (!tmp.getParentFile().exists() && !tmp.getParentFile().mkdirs()) {
            throw new IllegalArgumentException("Check you can write in " + tmp.getParentFile());
        }

        try (final JarOutputStream jar = new JarOutputStream(new FileOutputStream(tmp))) {
            jar.putNextEntry(new ZipEntry(Constants.DEPENDENCIES_LIST_RESOURCE_PATH));
            jar.write(dependencies.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        toClean.add(tmp);
        return tmp;
    }

    @Override
    protected void after() {
        toClean.forEach(f -> {
            if (!f.delete()) { // on win it can fail
                f.deleteOnExit();
            }
        });
    }
}
