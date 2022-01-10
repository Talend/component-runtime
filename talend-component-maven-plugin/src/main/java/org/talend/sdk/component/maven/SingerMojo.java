/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static com.google.cloud.tools.jib.api.buildplan.FilePermissions.DEFAULT_FILE_PERMISSIONS;
import static com.google.cloud.tools.jib.api.buildplan.FilePermissions.DEFAULT_FOLDER_PERMISSIONS;
import static java.util.Optional.ofNullable;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.maven.docker.JibHelper;
import org.talend.sdk.component.path.PathFactory;

@Audience(TALEND_INTERNAL)
@Mojo(name = "singer", defaultPhase = PACKAGE, threadSafe = true, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class SingerMojo extends AudienceAwareMojo {

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.car",
            property = "talend.singer.car", alias = "talend.car.output")
    private File car;

    @Parameter(property = "talend.singer.fromImage", defaultValue = "openjdk:8-jre-alpine")
    private String fromImage;

    @Parameter(property = "talend.singer.creationTime")
    private String creationTime;

    @Parameter(property = "talend.singer.workingDirectory", defaultValue = "/opt/talend")
    private String workingDirectory;

    @Parameter(property = "talend.singer.toImage")
    private String toImage;

    @Parameter(property = "talend.singer.repository")
    private String repository;

    @Parameter
    private Map<String, String> environment;

    @Parameter
    private Map<String, String> labels;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "talend.singer.versionProperty")
    private String versionProperty;

    @Parameter(property = "talend.singer.dockerExecutable", alias = "talend-image.dockerExecutable")
    private File dockerExecutable;

    @Parameter(property = "talend.singer.layersCacheDirectory", alias = "talend-image.layersCacheDirectory",
            defaultValue = "${project.build.directory}/maven/build/cache")
    private File layersCacheDirectory;

    @Parameter(property = "talend.singer.laggyPushWorkaroundRetries", alias = "talend-image.layersCacheDirectory",
            defaultValue = "0")
    private int laggyPushWorkaround;

    @Parameter
    private Map<String, String> dockerEnvironment;

    @Component
    private SettingsDecrypter settingsDecrypter;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepositories;

    @Component
    private RepositorySystem repositorySystem;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        doExecute();
    }

    private void doExecute() throws MojoExecutionException {
        try (final JibHelper jibHelper = new JibHelper(getLog(), project.getBuild().getDirectory(),
                layersCacheDirectory, repository, dockerEnvironment, dockerExecutable, laggyPushWorkaround)) {
            jibHelper
                    .prepare(project.getArtifactId(), project.getVersion(), project.getProperties(), fromImage, toImage,
                            creationTime, workingDirectory,
                            () -> "/opt/talend/singer/component/" + project.getArtifactId(), environment, labels);

            configure(jibHelper);

            getLog().info("Creating the image (can be long)");

            jibHelper
                    .build("Talend Singer Maven Plugin",
                            () -> ofNullable(session.getSettings().getServer(repository))
                                    .map(it -> settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(it)))
                                    .map(SettingsDecryptionResult::getServer)
                                    .orElse(null));

            if (versionProperty != null) {
                jibHelper.setProperties(project, versionProperty);
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /*
     * TALEND_SINGER_COMPONENT_ARCHIVE=/opt/talend/components/mailio-1.0.0.car \
     * java -jar component-kitap-$KIT_VERSION-fatjar.jar \
     * --config config.json
     */
    private void configure(final JibHelper helper) {
        final JibContainerBuilder builder = helper.getBuilder();
        final AbsoluteUnixPath workingDirectory = helper.getWorkingDirectory();

        // 1. add the kitapp "main" + workdir (1 layer)
        final Path main = findMain();
        final Path fakeWorkDir =
                PathFactory.get(project.getBuild().getDirectory()).resolve("component_mojo_singer_work");
        if (!Files.exists(fakeWorkDir)) {
            try {
                Files.createDirectories(fakeWorkDir);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        final AbsoluteUnixPath targetWorkDir = workingDirectory.resolve("work/");
        final AbsoluteUnixPath targetMain = workingDirectory.resolve("lib/" + main.getFileName().toString());
        builder
                .addLayer(LayerConfiguration
                        .builder()
                        .addEntry(main, targetMain, DEFAULT_FILE_PERMISSIONS, helper.getCreationTime())
                        .addEntry(fakeWorkDir, targetWorkDir, DEFAULT_FOLDER_PERMISSIONS, helper.getCreationTime())
                        .build());
        builder.addEnvironmentVariable("TALEND_SINGER_WORK_DIR", targetWorkDir.toString());

        // 3. add the .car and setup it in the environment
        final AbsoluteUnixPath targetCarPath = workingDirectory.resolve("lib/" + car.getName());
        builder
                .addLayer(LayerConfiguration
                        .builder()
                        .addEntry(car.toPath(), targetCarPath, DEFAULT_FILE_PERMISSIONS, helper.getCreationTime())
                        .build());
        builder.addEnvironmentVariable("TALEND_SINGER_COMPONENT_ARCHIVE", targetCarPath.toString());

        // 4. set the entry point
        builder.setEntrypoint("java", "-jar", targetMain.toString());
    }

    private Path findMain() {
        final LocalRepositoryManager lrm = repositorySystemSession.getLocalRepositoryManager();
        final Artifact artifact = new DefaultArtifact(GAV.GROUP, "component-kitap", "fatjar", "jar", GAV.VERSION);
        final File location = new File(lrm.getRepository().getBasedir(), lrm.getPathForLocalArtifact(artifact));
        if (!location.exists()) {
            final ArtifactRequest artifactRequest =
                    new ArtifactRequest().setArtifact(artifact).setRepositories(remoteRepositories);
            try {
                final ArtifactResult result =
                        repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
                if (result.isMissing()) {
                    throw new IllegalStateException("Can't find " + artifact);
                }
                return result.getArtifact().getFile().toPath();
            } catch (final ArtifactResolutionException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return location.toPath();
    }
}
