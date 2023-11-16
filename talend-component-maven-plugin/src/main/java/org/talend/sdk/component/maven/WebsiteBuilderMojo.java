/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.DecoratedCollection;
import com.github.mustachejava.util.Wrapper;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.path.PathFactory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Audience(TALEND_INTERNAL)
@Mojo(name = "website", requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class WebsiteBuilderMojo extends ComponentDependenciesBase {

    @Parameter(defaultValue = "src/main/talend/website", property = "talend.website-builder.directory")
    protected String websiteDir;

    @Parameter(defaultValue = "${project.build.directory}/talend/website", property = "talend.website-builder.output")
    protected String output;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "true", property = "talend-website-builder.cleanBeforeGeneration")
    private boolean cleanBeforeGeneration;

    @Parameter(defaultValue = "false", property = "talend-website-builder.publishOnGithubPages")
    private boolean publishOnGithubPages;

    @Parameter(defaultValue = "talend-component-website", property = "talend-website-builder.githubPagesServerId")
    private String githubPagesServerId;

    @Parameter(defaultValue = "${project.scm.url}", property = "talend-website-builder.githubPagesUrl")
    private String githubPagesUrl;

    @Parameter(defaultValue = "refs/heads/gh-pages", property = "talend-website-builder.githubPagesBranch")
    private String githubPagesBranch;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private SettingsDecrypter settingsDecrypter;

    @Parameter
    private boolean skip;

    private Asciidoctor asciidoctor;

    @Override
    protected void doExecute() throws MojoFailureException {
        final List<Project> components = findProjectsWithCar();
        final String doneMarker = getClass().getName() + ".done";
        if (skip || Boolean.parseBoolean(project.getProperties().getProperty(doneMarker))) {
            return;
        }

        if (components.isEmpty()) {
            throw new MojoFailureException("No component found in: " + reactorProjects);
        }
        project.getProperties().setProperty(doneMarker, "true");

        final DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory() {

            {
                setObjectHandler(new ReflectionObjectHandler() {

                    @Override
                    public Wrapper find(final String name, final List<Object> scopes) {
                        final Wrapper wrapper = super.find(name, scopes);
                        return s -> {
                            final Object call = wrapper.call(s);
                            if (Collection.class.isInstance(call) && !DecoratedCollection.class.isInstance(call)) {
                                return new DecoratedCollection<>(Collection.class.cast(call));
                            }
                            return call;
                        };
                    }

                    @Override // java 11 support
                    protected AccessibleObject findMember(final Class sClass, final String name) {
                        if (sClass == String.class && "value".equals(name)) {
                            return null;
                        }
                        return super.findMember(sClass, name);
                    }
                });
            }

            @Override
            public void encode(final String value, final Writer writer) {
                try {
                    writer.write(value);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };

        final Path root = mkdirs(PathFactory.get(output));
        if (cleanBeforeGeneration) {
            try {
                deleteFolder(root);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }

        // for each project, generate its website in output+artifactId
        components.forEach(it -> generateComponent(it, root, mustacheFactory));

        // generate index from
        final Map<String, List<Project>> globalContext = singletonMap("components", components);
        generateDefault(globalContext, () -> {
            try {
                return Files.newBufferedWriter(root.resolve("index.html"));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }, mustacheFactory, "main-index");
        generateFolder(globalContext, root, project.getBasedir().toPath().resolve(websiteDir), mustacheFactory);

        if (publishOnGithubPages) {
            pulishGhPages(root);
        }
    }

    private void pulishGhPages(final Path root) {
        final Server server = session.getSettings().getServer(githubPagesServerId);
        final Server decryptedServer = ofNullable(server)
                .map(s -> settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(s)))
                .map(SettingsDecryptionResult::getServer)
                .orElse(server);

        final Path gitCloneBase =
                mkdirs(PathFactory.get(project.getBuild().getDirectory()).resolve(UUID.randomUUID().toString()));
        final UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider(decryptedServer.getUsername(), decryptedServer.getPassword());
        try (final Git git = Git
                .cloneRepository()
                .setCredentialsProvider(credentialsProvider)
                .setURI(githubPagesUrl)
                .setDirectory(gitCloneBase.toFile())
                .setBranchesToClone(singleton(githubPagesBranch))
                .setBranch(githubPagesBranch)
                .call()) {

            // clean up repo
            deleteFolder(gitCloneBase);

            // copy new files
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    final String relative = root.relativize(file).toString();
                    final Path target = gitCloneBase.resolve(relative);
                    mkdirs(target.getParent());
                    Files.copy(file, target);
                    return super.visitFile(file, attrs);
                }
            });

            final String message =
                    "Updating the website with version " + project.getVersion() + " // " + new Date().toString();
            git.add().addFilepattern(".").call();
            git.commit().setAll(true).setMessage(message).call();
            git.status().call();
            git.push().setCredentialsProvider(credentialsProvider).add(githubPagesBranch).call();
            getLog().info("Updated the website on " + ZonedDateTime.now());
        } catch (final GitAPIException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void deleteFolder(final Path base) throws IOException {
        Files.walkFileTree(base, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                return dir.getFileName().toString().startsWith(".git") ? FileVisitResult.SKIP_SUBTREE
                        : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                if (Files.list(dir).count() == 0) {
                    Files.delete(dir);
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    private void generateComponent(final Project project, final Path root, final MustacheFactory mustacheFactory) {
        final Path dir = mkdirs(root.resolve(project.project.getArtifactId()));

        // global pages
        generateFolder(project, dir, this.project.getBasedir().toPath().resolve(websiteDir), mustacheFactory);

        // specific component pages
        final Path templatesDir = project.project.getBasedir().toPath().resolve(websiteDir);
        generateFolder(project, dir, templatesDir, mustacheFactory);

        // ensure there is an index (main index can rely on that)
        if (!Files.exists(templatesDir.resolve("index.mustache"))) {
            generateDefault(project, () -> {
                try {
                    return Files.newBufferedWriter(dir.resolve("index.html"));
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }, mustacheFactory, "default-index");
        }
        if (!Files.exists(dir.resolve("documentation.html"))) {
            final Path doc = PathFactory
                    .get(project.project.getBuild().getOutputDirectory())
                    .resolve("TALEND-INF/documentation.adoc");
            if (Files.exists(doc)) {
                asciidoc2Html(doc, dir.resolve("documentation.html"), mustacheFactory);
            } else {
                getLog().warn("No documentation found, ensure to generate it before generating the site");
            }
        }

        // ensure the car is next to the html
        try {
            Files
                    .copy(project.car, dir.resolve(project.car.getFileName().toString()),
                            StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void asciidoc2Html(final Path doc, final Path target, final MustacheFactory mustacheFactory) {
        try {
            final String html = (asciidoctor == null ? asciidoctor = Asciidoctor.Factory.create() : asciidoctor)
                    .convert(new String(Files.readAllBytes(doc), StandardCharsets.UTF_8),
                            OptionsBuilder
                                    .options()
                                    .baseDir(doc.getParent().toFile())
                                    .backend("html5")
                                    .safe(SafeMode.SECURE)
                                    .docType("article")
                                    .mkDirs(true));
            generateDefault(singletonMap("html", html), () -> {
                try {
                    return Files.newBufferedWriter(target);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }, mustacheFactory, "default-layout");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void generateDefault(final Object context, final Supplier<Writer> writerSupplier,
            final MustacheFactory mustacheFactory, final String name) {
        try (final Reader tpl = new InputStreamReader(requireNonNull(findTemplate(name)), StandardCharsets.UTF_8);
                final Writer writer = writerSupplier.get()) {
            mustacheFactory.compile(tpl, name).execute(writer, context);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private InputStream findTemplate(final String name) {
        final Path override =
                this.project.getBasedir().toPath().resolve("templates").resolve(websiteDir).resolve(name + ".mustache");
        if (Files.exists(override)) {
            try {
                return Files.newInputStream(override);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        // built-in
        return Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(getClass().getSimpleName() + '/' + name + ".mustache");
    }

    private void generateFolder(final Object context, final Path dir, final Path templatesDir,
            final MustacheFactory mustacheFactory) {
        if (!Files.exists(templatesDir)) {
            return;
        }

        try {
            Files.list(templatesDir).filter(it -> it.endsWith(".mustache")).forEach(template -> {
                final String name = template.getFileName().toString();
                try (final Reader tpl = Files.newBufferedReader(template);
                        final Writer writer = Files
                                .newBufferedWriter(
                                        dir.resolve(name.substring(0, name.length() - "mustache".length()) + "html"))) {
                    mustacheFactory.compile(tpl, dir.getFileName().toString() + '_' + name).execute(writer, context);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        synchronizeAssets(templatesDir, dir);
    }

    private void synchronizeAssets(final Path templatesDir, final Path dir) {
        Stream.of("css", "js", "images").map(templatesDir::resolve).filter(Files::exists).forEach(src -> {
            try {
                Files.walkFileTree(src, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                            throws IOException {
                        final String relative = templatesDir.relativize(file).toString();
                        final Path target = dir.resolve(relative);
                        mkdirs(target.getParent());
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private List<Project> findProjectsWithCar() {
        return reactorProjects
                .stream()
                .filter(it -> "jar".equals(it.getPackaging()))
                .map(project -> new Project(project, toCarPath(project)))
                .filter(p -> Files.exists(p.car))
                .collect(toList());
    }

    private Path mkdirs(final Path root) {
        if (!Files.exists(root)) {
            try {
                Files.createDirectories(root);
            } catch (final IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return root;
    }

    private static Path toCarPath(final MavenProject project) {
        // default, see CarMojo
        return PathFactory.get(project.getBuild().getDirectory()).resolve(project.getBuild().getFinalName() + ".car");
    }

    @Getter
    @AllArgsConstructor
    private static class Project {

        private final MavenProject project;

        private final Path car;

        public String getCarName() {
            return car.getFileName().toString();
        }
    }
}
