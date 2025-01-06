/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.maven.docker;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.talend.sdk.component.maven.thread.MavenThreadFactory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JibHelper implements AutoCloseable {

    private final Log log;

    private final String tempDir;

    private final File layersCacheDirectory;

    private final String repository;

    private final Map<String, String> dockerEnvironment;

    private final File dockerExecutable;

    private final int laggyPushWorkaround;

    private final ExecutorService executor =
            Executors.newCachedThreadPool(new MavenThreadFactory(getClass().getName()));

    @Getter
    private JibContainerBuilder builder;

    @Getter
    private String tag;

    @Getter
    private String imageName;

    @Getter
    private String image;

    @Getter
    private AbsoluteUnixPath workingDirectory;

    @Getter
    private Instant creationTime;

    public void prepare(final String artifactId, final String projectVersion, final Properties projectProperties,
            final String fromImage, final String toImage, final String createTime, final String workDir,
            final Supplier<String> fallbackWorkingDir, final Map<String, String> environment,
            final Map<String, String> labels) throws InvalidImageReferenceException {
        if (builder != null) {
            throw new IllegalStateException("prepare had already been called");
        }

        builder = Jib.from(ImageReference.parse(fromImage));
        creationTime = createTime == null || createTime.trim().isEmpty() ? Instant.now() : Instant.parse(createTime);
        builder.setCreationTime(this.creationTime);
        workingDirectory = AbsoluteUnixPath.get(workDir == null ? fallbackWorkingDir.get() : workDir);
        builder.setWorkingDirectory(this.workingDirectory);
        if (environment != null) {
            builder.setEnvironment(environment);
        }

        tag = projectVersion.endsWith("-SNAPSHOT")
                ? projectVersion.replace("-SNAPSHOT", "") + "_"
                        + ofNullable(projectProperties.getProperty("git.branch"))
                                .map(it -> it.replace('/', '_') + '_')
                                .orElse("")
                        + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                : projectVersion;
        image = toImage == null ? artifactId : toImage;
        imageName = ((repository == null || repository.trim().isEmpty()) ? "" : (repository + '/')) + image + ':' + tag;

        if (labels != null) {
            builder
                    .setLabels(labels
                            .entrySet()
                            .stream()
                            .peek(it -> it.setValue(it.getValue().replace("@imageName@", imageName)))
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

    public void build(final String toolName, final Supplier<Server> credentialsSupplier)
            throws InvalidImageReferenceException, InterruptedException, ExecutionException, IOException,
            CacheDirectoryCreationException, RegistryException, MojoExecutionException {
        if (builder == null) {
            throw new IllegalStateException("prepare had not been called");
        }
        if (repository != null) { // push
            final Server credentials = credentialsSupplier.get();

            if (laggyPushWorkaround > 0) {
                toLocalDocker(executor, builder, tag, imageName, toolName);
                hackyPush(tag, repository, imageName, credentials);
            } else {
                final RegistryImage registryImage = RegistryImage.named(imageName);
                if (credentials != null) {
                    registryImage.addCredential(credentials.getUsername(), credentials.getPassword());
                }
                builder
                        .containerize(configureContainer(Containerizer.to(registryImage), executor,
                                "Talend Singer Maven Plugin"));
                log.info("Pushed image='" + imageName + "', tag='" + tag + "'");
            }
        } else {
            toLocalDocker(executor, builder, tag, imageName, toolName);
        }
    }

    public Containerizer configureContainer(final Containerizer to, final ExecutorService executor,
            final String toolName) {
        return to
                .setExecutorService(executor)
                .setApplicationLayersCache(layersCacheDirectory.toPath())
                .setBaseImageLayersCache(layersCacheDirectory.toPath())
                .setToolName(toolName);
    }

    public void toLocalDocker(final ExecutorService executor, final JibContainerBuilder builder, final String tag,
            final String imageName, final String toolName) throws InvalidImageReferenceException, InterruptedException,
            ExecutionException, IOException, CacheDirectoryCreationException, RegistryException {
        final DockerDaemonImage docker = DockerDaemonImage.named(imageName);
        if (dockerEnvironment != null) {
            docker.setDockerEnvironment(dockerEnvironment);
        }
        if (dockerExecutable != null) {
            docker.setDockerExecutable(dockerExecutable.toPath());
        }
        builder.containerize(configureContainer(Containerizer.to(docker), executor, toolName));
        log.info("Built local image='" + imageName + "', tag='" + tag + "'");
    }

    // until jib supports retries this is a CI workaround
    public void hackyPush(final String tag, final String repository, final String imageName, final Server credentials)
            throws IOException, InterruptedException, MojoExecutionException {
        log
                .warn("Using push workaround for nasty registries (using exec directly on 'docker'), "
                        + "it is highly recommended to not use laggyPushWorkaround configuration if you can");

        if (credentials != null) {
            final File credFile = new File(tempDir, "talend-component-docker-credentials.temp");
            credFile.getParentFile().mkdirs(); // should be useless but just in case
            try (final FileWriter writer = new FileWriter(credFile)) {
                writer.write(credentials.getPassword());
            }
            try {
                final ProcessBuilder processBuilder = new ProcessBuilder("docker", "login", repository, "--username",
                        credentials.getUsername(), "--password-stdin");
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                processBuilder.redirectInput(ProcessBuilder.Redirect.from(credFile));
                final int exitCode = processBuilder.start().waitFor();
                if (exitCode != 0) {
                    log.warn("Can't login, got status: " + exitCode);
                }
            } finally {
                if (!credFile.delete()) {
                    credFile.deleteOnExit();
                }
            }
        }

        boolean ok = false;
        for (int i = 0; i < laggyPushWorkaround; i++) {
            final int exit = new ProcessBuilder("docker", "push", imageName).inheritIO().start().waitFor();
            if (exit == 0) {
                ok = true;
                log.info("Pushed image='" + imageName + "', tag='" + tag + "'");
                break;
            } else {
                log.warn("Push #" + (i + 1) + " got " + exit + " exit status");
            }
        }
        if (!ok) {
            throw new MojoExecutionException("Push didn't succeed");
        }
    }

    public void setProperties(final MavenProject project, final String versionProperty) {
        final String repo = repository == null ? "" : repository;
        project.getProperties().put(versionProperty, imageName);
        project.getProperties().put(versionProperty + ".repository", repo);
        project.getProperties().put(versionProperty + ".repositoryPrefixed", repo.isEmpty() ? "" : repo + '/');
        project.getProperties().put(versionProperty + ".image", image);
        project.getProperties().put(versionProperty + ".version", tag);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
