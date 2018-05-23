/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.front;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.COMPONENT_MISSING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.DESIGN_MODEL_MISSING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.PLUGIN_MISSING;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.design.extension.DesignModel;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.extension.ComponentContexts;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.dao.ComponentFamilyDao;
import org.talend.sdk.component.server.front.base.internal.RequestKey;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentId;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.Dependencies;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.Icon;
import org.talend.sdk.component.server.front.model.Link;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.ActionsService;
import org.talend.sdk.component.server.service.ComponentManagerService;
import org.talend.sdk.component.server.service.IconResolver;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;
import org.talend.sdk.component.spi.component.ComponentExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("component")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComponentResource {

    private final ConcurrentMap<RequestKey, ComponentIndices> indicesPerRequest = new ConcurrentHashMap<>();

    @Inject
    private ComponentManager manager;

    @Inject
    private ComponentManagerService componentManagerService;

    @Inject
    private ComponentDao componentDao;

    @Inject
    private ComponentFamilyDao componentFamilyDao;

    @Inject
    private LocaleMapper localeMapper;

    @Inject
    private ActionsService actionsService;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private IconResolver iconResolver;

    @Inject
    private ComponentServerConfiguration configuration;

    @PostConstruct
    private void setupRuntime() {
        log.info("Initializing " + getClass());

        // preload some highly used data
        getIndex("en", false);
    }

    /**
     * Returns a list of dependencies for the given components.
     *
     * IMPORTANT: don't forget to add the component itself since it will not be part of the dependencies.
     *
     * Then you can use /dependency/{id} to download the binary.
     *
     * @param ids the list of component identifiers to find the dependencies for.
     * @return the list of dependencies per component.
     */
    @GET
    @Path("dependencies")
    public Dependencies getDependencies(@QueryParam("identifier") final String[] ids) {
        if (ids.length == 0) {
            return new Dependencies(emptyMap());
        }
        return new Dependencies(
                Stream.of(ids).map(id -> componentDao.findById(id)).collect(toMap(ComponentFamilyMeta.BaseMeta::getId,
                        meta -> componentManagerService.manager().findPlugin(meta.getParent().getPlugin()).map(c -> {
                            ComponentExtension.ComponentContext context =
                                    c.get(ComponentContexts.class).getContexts().get(meta.getType());
                            ComponentExtension extension = context.owningExtension();
                            final Stream<Artifact> deps = c.findDependencies();
                            final Stream<Artifact> artifacts;
                            if (configuration.addExtensionDependencies() && extension != null) {
                                final List<Artifact> dependencies = deps.collect(toList());
                                final Stream<Artifact> addDeps = extension
                                        .getAdditionalDependencies()
                                        .stream()
                                        .map(Artifact::from)
                                        // filter required artifacts if they are already present in the list.
                                        .filter(extArtifact -> dependencies
                                                .stream()
                                                .map(d -> d.getGroup() + ":" + d.getArtifact())
                                                .noneMatch(ga -> ga.equals(
                                                        extArtifact.getGroup() + ":" + extArtifact.getArtifact())));
                                artifacts = Stream.concat(dependencies.stream(), addDeps);
                            } else {
                                artifacts = deps;
                            }
                            return new DependencyDefinition(artifacts.map(Artifact::toCoordinate).collect(toList()));
                        }).orElse(new DependencyDefinition(emptyList())))));
    }

    /**
     * Return a binary of the dependency represented by `id`.
     * It can be maven coordinates for dependencies or a component id.
     *
     * @param id the dependency identifier.
     * @return the dependency binary (jar).
     */
    @GET
    @Path("dependency/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getDependency(@PathParam("id") final String id) {
        final ComponentFamilyMeta.BaseMeta<?> component = componentDao.findById(id);
        final File file;
        if (component != null) { // local dep
            file = componentManagerService
                    .manager()
                    .findPlugin(component.getParent().getPlugin())
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(PLUGIN_MISSING, "No plugin matching the id: " + id))
                            .build()))
                    .getContainerFile()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(PLUGIN_MISSING, "No dependency matching the id: " + id))
                            .build()));
        } else { // just try to resolve it locally, note we would need to ensure some security here
            // .map(Artifact::toPath).map(localDependencyRelativeResolver
            final Artifact artifact = Artifact.from(id);
            file = componentManagerService.manager().getContainer().resolve(artifact.toPath());
        }
        if (!file.exists()) {
            throw new WebApplicationException(Response
                    .status(Response.Status.NOT_FOUND)
                    .type(APPLICATION_JSON_TYPE)
                    .entity(new ErrorPayload(PLUGIN_MISSING, "No file found for: " + id))
                    .build());
        }
        return output -> {
            final byte[] buffer = new byte[40960]; // 5k
            try (final InputStream stream = new BufferedInputStream(new FileInputStream(file), buffer.length)) {
                int count;
                while ((count = stream.read(buffer)) >= 0) {
                    if (count == 0) {
                        continue;
                    }
                    output.write(buffer, 0, count);
                }
            }
        };
    }

    /**
     * Returns the list of available components.
     *
     * @param language the language for display names.
     * @param includeIconContent should the icon binary format be included in the payload.
     * @return the index of available components.
     */
    @GET
    @Path("index")
    public ComponentIndices getIndex(@QueryParam("language") @DefaultValue("en") final String language,
            @QueryParam("includeIconContent") @DefaultValue("false") final boolean includeIconContent) {
        final Locale locale = localeMapper.mapLocale(language);
        return indicesPerRequest.computeIfAbsent(new RequestKey(locale, includeIconContent),
                k -> new ComponentIndices(manager
                        .find(c -> c
                                .execute(
                                        () -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                                .flatMap(component -> Stream.concat(
                                        component.getPartitionMappers().values().stream().map(
                                                mapper -> toComponentIndex(c.getLoader(), locale, c.getId(), mapper,
                                                        c.get(ComponentManager.OriginalId.class), includeIconContent)),
                                        component
                                                .getProcessors()
                                                .values()
                                                .stream()
                                                .map(proc -> toComponentIndex(c.getLoader(), locale, c.getId(), proc,
                                                        c.get(ComponentManager.OriginalId.class),
                                                        includeIconContent)))))
                        .collect(toList())));
    }

    /**
     * Returns a particular family icon in raw bytes.
     *
     * @param id the family identifier.
     * @return the family icon in binary form.
     */
    @GET
    @Path("icon/family/{id}")
    public Response familyIcon(@PathParam("id") final String id) {
        // todo: add caching if SvgIconResolver becomes used a lot - not the case ATM
        final ComponentFamilyMeta meta = componentFamilyDao.findById(id);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No family for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
        final Optional<Container> plugin = manager.findPlugin(meta.getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.PLUGIN_MISSING,
                            "No plugin '" + meta.getPlugin() + "' for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get().getLoader(), meta.getIcon());
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for family identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    /**
     * Returns a particular component icon in raw bytes.
     *
     * @param id the component identifier.
     * @return the component icon in binary form.
     */
    @GET
    @Path("icon/{id}")
    public Response icon(@PathParam("id") final String id) {
        // todo: add caching if SvgIconResolver becomes used a lot - not the case ATM
        final ComponentFamilyMeta.BaseMeta<Object> meta = componentDao.findById(id);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No component for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final Optional<Container> plugin = manager.findPlugin(meta.getParent().getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.PLUGIN_MISSING,
                            "No plugin '" + meta.getParent().getPlugin() + "' for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get().getLoader(), meta.getIcon());
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    /**
     * Allows to migrate a component configuration without calling any component execution.
     *
     * @param id the component identifier.
     * @param version the configuration version you send.
     * @param config the actual configuration in key/value form.
     * @return the new configuration for that component (or the same if no migration was needed).
     */
    @POST
    @Path("migrate/{id}/{configurationVersion}")
    public Map<String, String> migrate(@PathParam("id") final String id,
            @PathParam("configurationVersion") final int version, final Map<String, String> config) {
        return ofNullable(componentDao.findById(id))
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "Didn't find component " + id))
                        .build()))
                .getMigrationHandler()
                .migrate(version, config);
    }

    /**
     * Returns the set of metadata about a few components identified by their 'id'.
     *
     * @param language the language for display names/placeholders/....
     * @param ids the component identifiers to request.
     * @return the list of details for the requested components.
     */
    @GET // TODO: max ids.length
    @Path("details") // bulk mode to avoid to fetch components one by one when reloading a pipeline/job
    public ComponentDetailList getDetail(@QueryParam("language") @DefaultValue("en") final String language,
            @QueryParam("identifiers") final String[] ids) {

        if (ids == null || ids.length == 0) {
            return new ComponentDetailList(emptyList());
        }

        final Map<String, ErrorPayload> errors = new HashMap<>();
        final List<ComponentDetail> details =
                Stream.of(ids).map(id -> ofNullable(componentDao.findById(id)).orElseGet(() -> {
                    errors.put(id, new ErrorPayload(COMPONENT_MISSING, "No component '" + id + "'"));
                    return null;
                })).filter(Objects::nonNull).map(meta -> {
                    final Optional<Container> plugin = manager.findPlugin(meta.getParent().getPlugin());
                    if (!plugin.isPresent()) {
                        errors.put(meta.getId(),
                                new ErrorPayload(PLUGIN_MISSING, "No plugin '" + meta.getParent().getPlugin() + "'"));
                        return null;
                    }

                    final Container container = plugin.get();

                    final Optional<DesignModel> model = ofNullable(meta.get(DesignModel.class));
                    if (!model.isPresent()) {
                        errors.put(meta.getId(),
                                new ErrorPayload(DESIGN_MODEL_MISSING, "No design model '" + meta.getId() + "'"));
                        return null;
                    }

                    final Locale locale = localeMapper.mapLocale(language);

                    final ComponentDetail componentDetail = new ComponentDetail();
                    componentDetail.setLinks(emptyList() /* todo ? */);
                    componentDetail.setId(createMetaId(container, meta));
                    componentDetail.setVersion(meta.getVersion());
                    componentDetail.setIcon(meta.getIcon());
                    componentDetail.setInputFlows(model.get().getInputFlows());
                    componentDetail.setOutputFlows(model.get().getOutputFlows());
                    componentDetail
                            .setType(ComponentFamilyMeta.ProcessorMeta.class.isInstance(meta) ? "processor" : "input");
                    componentDetail.setDisplayName(
                            meta.findBundle(container.getLoader(), locale).displayName().orElse(meta.getName()));
                    componentDetail.setProperties(propertiesService
                            .buildProperties(meta.getParameterMetas(), container.getLoader(), locale, null)
                            .collect(toList()));
                    componentDetail.setActions(
                            actionsService.findActions(meta.getParent().getName(), container, locale, meta));

                    return componentDetail;
                }).filter(Objects::nonNull).collect(toList());

        if (!errors.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
        }

        return new ComponentDetailList(details);
    }

    private ComponentId createMetaId(final Container container, final ComponentFamilyMeta.BaseMeta<Object> meta) {
        return new ComponentId(meta.getId(), meta.getParent().getId(), meta.getParent().getPlugin(),
                ofNullable(container.get(ComponentManager.OriginalId.class))
                        .map(ComponentManager.OriginalId::getValue)
                        .orElse(container.getId()),
                meta.getParent().getName(), meta.getName());
    }

    private ComponentIndex toComponentIndex(final ClassLoader loader, final Locale locale, final String plugin,
            final ComponentFamilyMeta.BaseMeta meta, final ComponentManager.OriginalId originalId,
            final boolean includeIcon) {
        final String icon = meta.getIcon();
        final String familyIcon = meta.getParent().getIcon();
        final IconResolver.Icon iconContent = iconResolver.resolve(loader, icon);
        final IconResolver.Icon iconFamilyContent = iconResolver.resolve(loader, familyIcon);
        return new ComponentIndex(
                new ComponentId(meta.getId(), meta.getParent().getId(), plugin,
                        ofNullable(originalId).map(ComponentManager.OriginalId::getValue).orElse(plugin),
                        meta.getParent().getName(), meta.getName()),
                meta.findBundle(loader, locale).displayName().orElse(meta.getName()),
                meta.getParent().findBundle(loader, locale).displayName().orElse(meta.getParent().getName()),
                new Icon(icon, iconContent == null ? null : iconContent.getType(),
                        !includeIcon ? null : (iconContent == null ? null : iconContent.getBytes())),
                new Icon(familyIcon, iconFamilyContent == null ? null : iconFamilyContent.getType(),
                        !includeIcon ? null : (iconFamilyContent == null ? null : iconFamilyContent.getBytes())),
                meta.getVersion(), meta.getParent().getCategories(), singletonList(new Link("Detail",
                        "/component/details?identifiers=" + meta.getId(), MediaType.APPLICATION_JSON)));
    }
}
