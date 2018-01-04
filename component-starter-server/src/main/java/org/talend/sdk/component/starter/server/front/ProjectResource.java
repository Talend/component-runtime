/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.front;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.talend.sdk.component.starter.server.configuration.StarterConfiguration;
import org.talend.sdk.component.starter.server.model.ErrorMessage;
import org.talend.sdk.component.starter.server.model.FactoryConfiguration;
import org.talend.sdk.component.starter.server.model.GithubProject;
import org.talend.sdk.component.starter.server.model.ProjectModel;
import org.talend.sdk.component.starter.server.model.Result;
import org.talend.sdk.component.starter.server.model.github.CreateProjectRequest;
import org.talend.sdk.component.starter.server.model.github.CreateProjectResponse;
import org.talend.sdk.component.starter.server.service.ProjectGenerator;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("project")
@ApplicationScoped
public class ProjectResource {

    @Inject
    private ProjectGenerator generator;

    @Inject
    private StarterConfiguration starterConfiguration;

    private FactoryConfiguration configuration;

    @PostConstruct
    private void init() {
        final List<String> buildTypes = new ArrayList<>(generator.getGenerators().keySet());
        buildTypes.sort(String::compareTo);

        final Map<String, List<FactoryConfiguration.Facet>> facets =
                generator.getFacets().values().stream().collect(toMap(e -> e.category().getHumanName(),
                        e -> new ArrayList<>(singletonList(new FactoryConfiguration.Facet(e.name(), e.description()))),
                        (u, u2) -> {
                            if (u == null) {
                                return u2;
                            }
                            u.addAll(u2);
                            return u;
                        }, TreeMap::new));
        facets.forEach((k, v) -> v.sort(Comparator.comparing(FactoryConfiguration.Facet::getName)));

        configuration = new FactoryConfiguration(buildTypes, facets);
    }

    @GET
    @Path("configuration")
    @Produces(MediaType.APPLICATION_JSON)
    public FactoryConfiguration getConfiguration() {
        return configuration;
    }

    @POST
    @Path("github")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result createOnGithub(final GithubProject project) {
        // create an in-memory zip of the project
        final ByteArrayOutputStream zip = new ByteArrayOutputStream();
        generator.generate(toRequest(project.getModel()), zip);

        final GithubProject.Repository githubConfig = project.getRepository();
        final boolean useOrganization = githubConfig.isUseOrganization();
        final String organization = useOrganization ? githubConfig.getOrganization() : githubConfig.getUsername();

        // create the github project
        final Client client = ClientBuilder.newClient();
        try {
            client
                    .target(starterConfiguration.githubBaseApi())
                    .path(useOrganization ? starterConfiguration.githubOrgCreateProjectPath()
                            : starterConfiguration.githubCreateProjectPath())
                    .resolveTemplate("name", organization)
                    .request(APPLICATION_JSON_TYPE)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Authorization",
                            "Basic " + Base64
                                    .getEncoder()
                                    .encodeToString((githubConfig.getUsername() + ':' + githubConfig.getPassword())
                                            .getBytes(StandardCharsets.UTF_8)))
                    .method(starterConfiguration.githubCreateProjectMethod(),
                            entity(new CreateProjectRequest(project.getModel().getArtifact(),
                                    project.getModel().getDescription(), false), APPLICATION_JSON_TYPE),
                            CreateProjectResponse.class);
        } finally {
            client.close();
        }

        // clone the project in a temp repo
        final File workDir = new File(
                starterConfiguration.workDir().replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir")),
                githubConfig.getRepository() + "_" + System.nanoTime());
        if (!workDir.mkdirs()) {
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage("can't create a temporary folder"))
                    .type(APPLICATION_JSON_TYPE)
                    .build());
        }

        final UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider(githubConfig.getUsername(), githubConfig.getPassword());
        try (final Git git = Git
                .cloneRepository()
                .setBranch("master")
                .setURI(String.format(starterConfiguration.githubRepository(), organization,
                        githubConfig.getRepository()))
                .setDirectory(workDir)
                .setProgressMonitor(NullProgressMonitor.INSTANCE)
                .setCredentialsProvider(credentialsProvider)
                .call()) {

            { // copy the zip files into the project temporary folder
                try (final ZipInputStream file = new ZipInputStream(new ByteArrayInputStream(zip.toByteArray()))) {
                    ZipEntry entry;
                    while ((entry = file.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }

                        final InputStream in = new BufferedInputStream(file) {

                            @Override
                            public void close() throws IOException {
                                file.closeEntry();
                            }
                        };

                        // drop the root folder to import it directly into the repo
                        final String path = entry.getName().substring(
                                ofNullable(project.getModel().getArtifact()).orElse("application").length() + 1);
                        final File out = new File(workDir, path);
                        out.getParentFile().mkdirs();

                        // we need to filter some files, we can filter more files later but for now it is not
                        // needed
                        // see codenvy facet for more details
                        if (path.equals("README.adoc") || path.endsWith(".json")) {
                            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                                final String content = reader.lines().collect(joining("\n"));
                                Files.write(out.toPath(),
                                        content
                                                .replace("@organization@", organization)
                                                .replace("@repository@", githubConfig.getRepository())
                                                .getBytes(StandardCharsets.UTF_8),
                                        StandardOpenOption.CREATE_NEW);
                            }
                        } else {
                            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }

            // add a gitignore, we could use the template but the default doesn't match what we want
            // see https://github.com/github/gitignore/
            final File gitIgnore = new File(workDir, ".gitignore");
            if (!gitIgnore.exists()) {
                try (final Writer writer = new FileWriter(gitIgnore)) {
                    switch (ofNullable(project.getModel().getBuildType()).orElse("Maven").toLowerCase(ENGLISH)) {
                    case "gradle":
                        writer.write(".gradle\n" + "/build/\n" + "\n" + "# Ignore Gradle GUI config\n"
                                + "gradle-app.setting\n" + "\n"
                                + "# Avoid ignoring Gradle wrapper jar file (.jar files are usually ignored)\n"
                                + "!gradle-wrapper.jar\n" + "\n" + "# Cache of project\n" + ".gradletasknamecache\n"
                                + "\n" + "# # Work around https://youtrack.jetbrains.com/issue/IDEA-116898\n"
                                + "# gradle/wrapper/gradle-wrapper.properties\n");

                        break;
                    default: // maven
                        writer.write(
                                "target/\n" + "pom.xml.tag\n" + "pom.xml.releaseBackup\n" + "pom.xml.versionsBackup\n"
                                        + "pom.xml.next\n" + "release.properties\n" + "dependency-reduced-pom.xml\n"
                                        + "buildNumber.properties\n" + ".mvn/timing.properties\n");
                    }
                }
            }

            // commit them all and push
            git.add().addFilepattern(".").call();
            git
                    .commit()
                    .setMessage("Importing Talend Component Project from the Starter application")
                    .setAuthor("Talend Component Kit Starter WebApp", "tacokit@talend.com")
                    .call();
            git
                    .push()
                    .setProgressMonitor(NullProgressMonitor.INSTANCE)
                    .setCredentialsProvider(credentialsProvider)
                    .setTimeout((int) TimeUnit.MINUTES.toSeconds(5))
                    .call();

        } catch (final GitAPIException | IOException e) {
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage(e.getMessage()))
                    .type(APPLICATION_JSON_TYPE)
                    .build());
        } finally {
            try {
                FileUtils.delete(workDir, FileUtils.RECURSIVE);
            } catch (final IOException e) {
                log.warn(e.getMessage(), e);
            }
        }

        return new Result(true);
    }

    @POST
    @Path("zip")
    @Produces("application/zip")
    @Consumes(MediaType.APPLICATION_JSON)
    public StreamingOutput createZip(final ProjectModel model) {
        return out -> {
            generator.generate(toRequest(model), out);
            out.flush();
        };
    }

    private ProjectRequest toRequest(final ProjectModel model) {
        String packageBase = ofNullable(model.getGroup()).orElse("com.component").replace('/', '.');
        if (packageBase.endsWith(".")) {
            packageBase = packageBase.substring(0, packageBase.length() - 1);
        }
        if (packageBase.isEmpty()) {
            packageBase = "component";
        }

        return new ProjectRequest(ofNullable(model.getBuildType()).orElse("maven").toLowerCase(Locale.ENGLISH),
                new ProjectRequest.BuildConfiguration(
                        ofNullable(model.getName()).orElse("A Talend generated Component Starter Project"),
                        ofNullable(model.getDescription())
                                .orElse("An application generated by the Talend Component Kit Starter"),
                        "jar", packageBase, ofNullable(model.getArtifact()).orElse("application"),
                        ofNullable(model.getVersion()).orElse("0.0.1-SNAPSHOT"), "1.8"),
                ofNullable(model.getPackageBase()).orElse("com.application").replace('/', '.'),
                ofNullable(model.getFacets()).orElse(emptyList()),
                ofNullable(model.getSources())
                        .map(s -> s
                                .stream()
                                .map(i -> new ProjectRequest.SourceConfiguration(i.getName(), i.getIcon(), i.isStream(),
                                        toStructure(false, i.getConfigurationStructure()).getStructure(),
                                        toStructure(i.isGenericOutput(), i.getOutputStructure())))
                                .collect(toList()))
                        .orElse(emptyList()),
                ofNullable(
                        model.getProcessors())
                                .map(s -> s
                                        .stream()
                                        .map(i -> new ProjectRequest.ProcessorConfiguration(i.getName(), i.getIcon(),
                                                toStructure(false, i.getConfigurationStructure()).getStructure(),
                                                ofNullable(i.getInputStructures())
                                                        .map(is -> is.stream().collect(
                                                                toMap(n -> unifiedName(n.getName()),
                                                                        nm -> toStructure(nm.isGeneric(),
                                                                                nm.getStructure()))))
                                                        .orElse(emptyMap()),
                                                ofNullable(i.getOutputStructures())
                                                        .map(is -> is
                                                                .stream()
                                                                .collect(toMap(n -> unifiedName(n.getName()),
                                                                        nm -> toStructure(nm.isGeneric(),
                                                                                nm.getStructure()))))
                                                        .orElse(emptyMap())))
                                        .collect(toList()))
                                .orElse(emptyList()),
                model.getFamily(), model.getCategory());
    }

    private String unifiedName(final String name) {
        if ("MAIN".equalsIgnoreCase(name)) {
            return "__default__";
        }
        return name;
    }

    private ProjectRequest.StructureConfiguration toStructure(final boolean generic, final ProjectModel.Model model) {
        return new ProjectRequest.StructureConfiguration(
                !generic ? new ProjectRequest.DataStructure(model == null || model.getEntries() == null ? emptyList()
                        : model
                                .getEntries()
                                .stream()
                                .map(e -> new ProjectRequest.Entry(e.getName(), e.getType(),
                                        e.getModel() != null ? toStructure(false, e.getModel()).getStructure() : null))
                                .collect(toList()))
                        : null,
                generic);
    }
}
