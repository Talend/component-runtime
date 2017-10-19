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
package org.talend.components.container.maven.shade;

import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import lombok.Setter;

public class ContainerDependenciesTransformer extends ArtifactTransformer {

    @Setter
    private String repositoryBase = "MAVEN-INF/repository/";

    @Setter
    private String ignoredPaths = "META-INF/";

    private Collection<String> ignoredPathsRuntime = null;

    @Override
    public void modifyOutputStream(final JarOutputStream jarOutputStream) throws IOException {
        final Collection<String> existing = new HashSet<>();
        artifacts.forEach(artifact -> {
            try {
                final String path = String.format("%s%s/%s/%s/%s-%s%s.%s", repositoryBase,
                        artifact.getGroupId().replace(".", "/"), artifact.getArtifactId(), artifact.getVersion(),
                        artifact.getArtifactId(), artifact.getVersion(),
                        ofNullable(artifact.getClassifier()).map(c -> '-' + c).orElse(""),
                        ofNullable(artifact.getType()).orElse("jar"));
                final StringBuilder current = new StringBuilder();
                final String[] parts = path.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    current.append(parts[i]).append("/");
                    final String folderPath = current.toString();
                    if (!isExcluded(folderPath) && existing.add(folderPath)) {
                        try {
                            jarOutputStream.putNextEntry(new ZipEntry(folderPath));
                        } catch (final IOException ioe) {
                            // already existing, skip
                        }
                    }
                }
                if (existing.add(path)) {
                    if (artifact.getFile() == null) {
                        throw new IllegalArgumentException("No file specified for " + artifact);
                    }
                    jarOutputStream.putNextEntry(new ZipEntry(path));
                    Files.copy(artifact.getFile().toPath(), jarOutputStream);
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    // we can't "putNextEntry" twice with a JarOutputStream so ignore default ones
    private boolean isExcluded(final String folderPath) {
        if (ignoredPathsRuntime == null) {
            ignoredPathsRuntime = ignoredPaths == null ? emptySet()
                    : of(ignoredPaths).map(p -> p.split(",")).map(Stream::of).orElseGet(Stream::empty).collect(toSet());
        }
        return ignoredPathsRuntime.contains(folderPath);
    }
}
