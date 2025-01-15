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
package org.talend.sdk.component.starter.server.front;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
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
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.starter.server.configuration.StarterConfiguration;
import org.talend.sdk.component.starter.server.model.ErrorMessage;
import org.talend.sdk.component.starter.server.model.FactoryConfiguration;
import org.talend.sdk.component.starter.server.model.ProjectModel;
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
