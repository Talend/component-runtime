/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.maven;

import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.PUBLIC;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.tools.ScanTask;

/**
 * Pre-scan the components and services in current component.
 */
@Audience(PUBLIC)
@Mojo(name = "scan-descriptor", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class ScanDescriptorMojo extends ClasspathMojoBase {

    @Parameter(defaultValue = "${project.build.outputDirectory}/TALEND-INF/scanning.properties",
            property = "talend.scan.output")
    private File output;

    @Parameter(property = "talend.scan.scannedDirectories")
    private Collection<File> scannedDirectories;

    @Parameter(property = "talend.scan.scannedDependencies")
    private Collection<String> scannedDependencies;

    @Parameter(property = "talend.scan.excludes")
    private List<String> excludes;

    @Parameter(property = "talend.excludes")
    private Collection<String> sharedExcludes;

    @Parameter(property = "talend.scan.includes")
    private List<String> includes;

    @Parameter(property = "talend.scan.filter.strategy", defaultValue = "exclude-include")
    private String filterStrategy;

    @Override
    public void doExecute() {
        new ScanTask(Stream.concat(getDirectoriesToScan(), getJarToScan(scannedDependencies)).collect(toList()),
                getExcludes(excludes, sharedExcludes), includes, filterStrategy, output).run();
    }

    private Stream<File> getDirectoriesToScan() {
        return scannedDirectories == null || scannedDirectories.isEmpty() ? Stream.of(classes)
                : scannedDirectories.stream();
    }
}
