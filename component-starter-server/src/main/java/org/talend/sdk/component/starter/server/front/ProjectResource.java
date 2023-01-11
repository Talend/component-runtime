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
package org.talend.sdk.component.starter.server.front;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
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
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;
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

    private static final Annotation[] NO_ANNOTATION = new Annotation[0];

    @Inject
    private ProjectGenerator generator;

    @Inject
    private StarterConfiguration starterConfiguration;

    private FactoryConfiguration configuration;

    @PostConstruct
    private void init() {
        final List<String> buildTypes = new ArrayList<>(generator.getGenerators().keySet());
        buildTypes.sort(String::compareTo);

        final Map<String, List<FactoryConfiguration.Facet>> facets = generator
                .getFacets()
                .values()
                .stream()
                .collect(toMap(e -> e.category().getHumanName(),
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
        gitPush(project, zip.toByteArray());
        return new Result(true);
    }

    @POST
    @Path("openapi/github")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result createOpenAPIOnGithub(final GithubProject project) {
        final ByteArrayOutputStream zip = new ByteArrayOutputStream();
        generator.generateFromOpenAPI(toRequest(project.getModel()), zip);
        gitPush(project, zip.toByteArray());
        return new Result(true);
    }

    @POST
    @Path("zip/form")
    @Produces("application/zip")
    public Response createZip(@FormParam("project") final String compressedModel, @Context final Providers providers) {
        final ProjectModel model = readProjectModel(compressedModel, providers);
        final String filename = ofNullable(model.getArtifact()).orElse("zip") + ".zip";
        return Response.ok().entity((StreamingOutput) out -> {
            generator.generate(toRequest(model), out);
            out.flush();
        }).header("Content-Disposition", "inline; filename=" + filename).build();
    }

    @POST
    @Path("openapi/zip/form")
    @Produces("application/zip")
    public Response createOpenAPIZip(@FormParam("project") final String compressedModel,
            @Context final Providers providers) {
        final ProjectModel model = readProjectModel(compressedModel, providers);
        final String filename = ofNullable(model.getArtifact()).orElse("zip") + ".zip";
        return Response.ok().entity((StreamingOutput) out -> {
            generator.generateFromOpenAPI(toRequest(model), out);
            out.flush();
        }).header("Content-Disposition", "inline; filename=" + filename).build();
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

    @POST
    @Path("openapi/zip")
    @Produces("application/zip")
    @Consumes(MediaType.APPLICATION_JSON)
    public StreamingOutput createOpenAPIZip(final ProjectModel model) {
        return out -> {
            generator.generateFromOpenAPI(toRequest(model), out);
            out.flush();
        };
    }

    private ProjectModel readProjectModel(final String compressedModel, final Providers providers) {
        final MessageBodyReader<ProjectModel> jsonReader = providers
                .getMessageBodyReader(ProjectModel.class, ProjectModel.class, NO_ANNOTATION, APPLICATION_JSON_TYPE);
        final ProjectModel model;
        try (final InputStream gzipInputStream = new ByteArrayInputStream(debase64(compressedModel))) {
            model = jsonReader
                    .readFrom(ProjectModel.class, ProjectModel.class, NO_ANNOTATION, APPLICATION_JSON_TYPE,
                            new MultivaluedHashMap<>(), gzipInputStream);
        } catch (final IOException e) {
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage(e.getMessage()))
                    .type(APPLICATION_JSON_TYPE)
                    .build());
        }
        return model;
    }

    private byte[] debase64(final String compressedModel) {
        try {
            return Base64.getUrlDecoder().decode(compressedModel);
        } catch (final IllegalArgumentException iae) {
            return Base64.getDecoder().decode(compressedModel);
        }
    }

    private void gitPush(final GithubProject project, final byte[] zip) {
        final GithubProject.Repository githubConfig = project.getRepository();
        final boolean useOrganization = githubConfig.isUseOrganization();
        final String organization = useOrganization ? githubConfig.getOrganization() : githubConfig.getUsername();

        // create the github project
        final Client client = ClientBuilder.newClient();
        try {
            client
                    .target(starterConfiguration.getGithubBaseApi())
                    .path(useOrganization ? starterConfiguration.getGithubOrgCreateProjectPath()
                            : starterConfiguration.getGithubCreateProjectPath())
                    .resolveTemplate("name", organization)
                    .request(APPLICATION_JSON_TYPE)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Authorization",
                            "Basic " + Base64
                                    .getEncoder()
                                    .encodeToString((githubConfig.getUsername() + ':' + githubConfig.getPassword())
                                            .getBytes(StandardCharsets.UTF_8)))
                    .method(starterConfiguration.getGithubCreateProjectMethod(),
                            entity(new CreateProjectRequest(
                                    githubConfig.getRepository(),
                                    String.format("%s :: %s",
                                            project.getModel().getArtifact(),
                                            project.getModel().getDescription()),
                                    false),
                                    APPLICATION_JSON_TYPE),
                            CreateProjectResponse.class);
        } catch (Exception e) {
            log.error("[gitPush] {}", e.getMessage());
            throw new WebApplicationException(Response
                    .status(500, e.getMessage())
                    .entity(new ErrorMessage(e.getMessage()))
                    .type(APPLICATION_JSON_TYPE)
                    .build());
        } finally {
            client.close();
        }

        // clone the project in a temp repo
        final File workDir = new File(
                starterConfiguration.getWorkDir().replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir")),
                githubConfig.getRepository() + "_" + System.nanoTime());
        if (!workDir.mkdirs()) {
            log.error("[gitPush] can't create a temporary folder");
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorMessage("can't create a temporary folder"))
                    .type(APPLICATION_JSON_TYPE)
                    .build());
        }

        final UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider(githubConfig.getUsername(), githubConfig.getPassword());
        // mainly for testing on local running meecrowave server
        try {
            SystemReader.getInstance().getUserConfig().clear();
        } catch (ConfigInvalidException | IOException e) {
            log.warn("Clear user gitconfig failed.");
        }
        try (final Git git = Git
                .cloneRepository()
                .setBranch("master")
                .setURI(String
                        .format(starterConfiguration.getGithubRepository(), organization, githubConfig.getRepository()))
                .setDirectory(workDir)
                .setProgressMonitor(NullProgressMonitor.INSTANCE)
                .setCredentialsProvider(credentialsProvider)
                .call()) {
            { // copy the zip files into the project temporary folder
                try (final ZipInputStream file = new ZipInputStream(new ByteArrayInputStream(zip))) {
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
                        final String path = entry
                                .getName()
                                .substring(ofNullable(project.getModel().getArtifact()).orElse("application").length()
                                        + 1);
                        final File out = new File(workDir, path);
                        if (!out.getCanonicalPath().startsWith(workDir.getCanonicalPath())) {
                            throw new IOException("The output file is not contained in the destination directory");
                        }
                        out.getParentFile().mkdirs();

                        // we need to filter some files, we can filter more files later but for now it is not
                        // needed
                        // see codenvy facet for more details
                        if (path.equals("README.adoc") || path.endsWith(".json")) {
                            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                                final String content = reader.lines().collect(joining("\n"));
                                Files
                                        .write(out.toPath(),
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
                        writer
                                .write(".gradle\n" + "/build/\n" + "\n" + "# Ignore Gradle GUI config\n"
                                        + "gradle-app.setting\n" + "\n"
                                        + "# Avoid ignoring Gradle wrapper jar file (.jar files are usually ignored)\n"
                                        + "!gradle-wrapper.jar\n" + "\n" + "# Cache of project\n"
                                        + ".gradletasknamecache\n" + "\n"
                                        + "# # Work around https://youtrack.jetbrains.com/issue/IDEA-116898\n"
                                        + "# gradle/wrapper/gradle-wrapper.properties\n");

                        break;
                    default: // maven
                        writer
                                .write("target/\n" + "pom.xml.tag\n" + "pom.xml.releaseBackup\n"
                                        + "pom.xml.versionsBackup\n" + "pom.xml.next\n" + "release.properties\n"
                                        + "dependency-reduced-pom.xml\n" + "buildNumber.properties\n"
                                        + ".mvn/timing.properties\n");
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
            log.error("[gitPush] {}", e.getMessage());
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
    }

    private ProjectRequest toRequest(final ProjectModel model) {
        String group = ofNullable(model.getGroup()).orElse("com.component").replace('/', '.');
        if (group.endsWith(".")) {
            group = group.substring(0, group.length() - 1);
        }
        if (group.isEmpty()) {
            group = "component";
        }

        final String rootPackage = ofNullable(model.getPackageBase()).orElse("com.application").replace('/', '.');

        // first datastores cause they are reused in datasets and finally the config which reuses both config types
        final Map<String, ProjectRequest.ReusableConfiguration> reusableConfigs = ofNullable(model.getDatastores())
                .map(Collection::stream)
                .map(rs -> this.mapReusableConfig(rs, "datastore", rootPackage, new HashMap<>()))
                .orElseGet(Stream::empty)
                .collect(toMap(ProjectRequest.ReusableConfiguration::getId, identity()));

        reusableConfigs
                .putAll(ofNullable(model.getDatasets())
                        .map(Collection::stream)
                        .map(rs -> this.mapReusableConfig(rs, "dataset", rootPackage, reusableConfigs))
                        .orElseGet(Stream::empty)
                        .collect(toMap(ProjectRequest.ReusableConfiguration::getId, identity())));

        return new ProjectRequest(ofNullable(model.getBuildType()).orElse("maven").toLowerCase(Locale.ENGLISH),
                new ProjectRequest.BuildConfiguration(
                        ofNullable(model.getName()).orElse("A Talend generated Component Starter Project"),
                        ofNullable(model.getDescription())
                                .orElse("An application generated by the Talend Component Kit Starter"),
                        "jar", group, ofNullable(model.getArtifact()).orElse("application"),
                        ofNullable(model.getVersion()).orElse("0.0.1-SNAPSHOT"), "1.8"),
                rootPackage, ofNullable(model.getFacets()).orElse(emptyList()),
                ofNullable(model.getSources())
                        .map(s -> s
                                .stream()
                                .map(i -> new ProjectRequest.SourceConfiguration(i.getName(), i.getIcon(), i.isStream(),
                                        toStructure(false, i.getConfigurationStructure(), true, reusableConfigs)
                                                .getStructure(),
                                        toStructure(i.isGenericOutput(), i.getOutputStructure(), false,
                                                reusableConfigs)))
                                .collect(toList()))
                        .orElse(emptyList()),
                ofNullable(model.getProcessors())
                        .map(s -> s
                                .stream()
                                .map(i -> new ProjectRequest.ProcessorConfiguration(i.getName(), i.getIcon(),
                                        toStructure(false, i.getConfigurationStructure(), true, reusableConfigs)
                                                .getStructure(),
                                        mapStructures(i.getInputStructures(), reusableConfigs),
                                        mapStructures(i.getOutputStructures(), reusableConfigs)))
                                .collect(toList()))
                        .orElse(emptyList()),
                reusableConfigs.values(), model.getFamily(), model.getCategory(), model.getOpenapi());
    }

    private Stream<ProjectRequest.ReusableConfiguration> mapReusableConfig(
            final Stream<ProjectModel.ReusableStructure> rs, final String type, final String basePck,
            final Map<String, ProjectRequest.ReusableConfiguration> reusableConfigs) {
        return rs
                .map(it -> new ProjectRequest.ReusableConfiguration(it.getId(),
                        basePck + '.' + type + '.' + ensureUnAmbiguousName(type, it.getName()),
                        toStructure(false, it.getStructure(), true, reusableConfigs).getStructure(), type));
    }

    private String ensureUnAmbiguousName(final String type, final String name) {
        if (type.equalsIgnoreCase(name)) {
            return "Custom" + name;
        }
        return name;
    }

    private Map<String, ProjectRequest.StructureConfiguration> mapStructures(
            final Collection<ProjectModel.NamedModel> inputStructures,
            final Map<String, ProjectRequest.ReusableConfiguration> reusableConfigs) {
        return ofNullable(inputStructures)
                .map(is -> is
                        .stream()
                        .collect(toMap(n -> unifiedName(n.getName()),
                                nm -> toStructure(nm.isGeneric(), nm.getStructure(), false, reusableConfigs))))
                .orElse(emptyMap());
    }

    private String unifiedName(final String name) {
        if ("MAIN".equalsIgnoreCase(name)) {
            return "__default__";
        }
        return name;
    }

    private ProjectRequest.StructureConfiguration toStructure(final boolean generic, final ProjectModel.Model model,
            final boolean rootConfiguration, final Map<String, ProjectRequest.ReusableConfiguration> reusableConfigs) {
        if (generic) {
            return new ProjectRequest.StructureConfiguration(null, generic);
        }
        final boolean hasEntries = !(model == null || model.getEntries() == null);
        if (!hasEntries) {
            if (rootConfiguration) {
                return new ProjectRequest.StructureConfiguration(new ProjectRequest.DataStructure(new ArrayList<>()),
                        generic);
            }
            return new ProjectRequest.StructureConfiguration(null, generic);
        }
        return new ProjectRequest.StructureConfiguration(
                new ProjectRequest.DataStructure(model.getEntries().stream().map(e -> {
                    if (e.getReference() != null && !e.getReference().isEmpty()) {
                        final ProjectRequest.ReusableConfiguration reusableConfiguration =
                                reusableConfigs.get(e.getReference());
                        if (reusableConfiguration == null) {
                            return null; // todo: throw 400?
                        }
                        return new ProjectRequest.Entry(e.getName(), reusableConfiguration.getName(), e.getReference(),
                                null);
                    }
                    return new ProjectRequest.Entry(e.getName(), e.getType(), e.getReference(),
                            e.getModel() != null
                                    ? toStructure(false, e.getModel(), false, reusableConfigs).getStructure()
                                    : null);
                }).filter(Objects::nonNull).collect(toList())), generic);
    }
}
