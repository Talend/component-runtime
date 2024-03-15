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
package org.talend.sdk.component.server.front;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_SVG_XML_TYPE;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.COMPONENT_MISSING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.DESIGN_MODEL_MISSING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.PLUGIN_MISSING;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.design.extension.DesignModel;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.internationalization.ComponentBundle;
import org.talend.sdk.component.runtime.internationalization.FamilyBundle;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.BaseMeta;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.PartitionMapperMeta;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta.ProcessorMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.extension.ComponentContexts;
import org.talend.sdk.component.server.api.ComponentResource;
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
import org.talend.sdk.component.server.front.model.IconSymbol;
import org.talend.sdk.component.server.front.model.Link;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.front.security.SecurityUtils;
import org.talend.sdk.component.server.lang.MapCache;
import org.talend.sdk.component.server.service.ActionsService;
import org.talend.sdk.component.server.service.ComponentManagerService;
import org.talend.sdk.component.server.service.ExtensionComponentMetadataManager;
import org.talend.sdk.component.server.service.IconResolver;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;
import org.talend.sdk.component.server.service.SimpleQueryLanguageCompiler;
import org.talend.sdk.component.server.service.VirtualDependenciesService;
import org.talend.sdk.component.server.service.event.DeployedComponent;
import org.talend.sdk.component.server.service.jcache.FrontCacheKeyGenerator;
import org.talend.sdk.component.server.service.jcache.FrontCacheResolver;
import org.talend.sdk.component.spi.component.ComponentExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@CacheDefaults(cacheResolverFactory = FrontCacheResolver.class, cacheKeyGenerator = FrontCacheKeyGenerator.class)
public class ComponentResourceImpl implements ComponentResource {

    public static final String BASE64_PREFIX = "base64://"; //$NON-NLS-1$

    public static final String COMPONENT_TYPE_STANDALONE = "standalone";

    public static final String COMPONENT_TYPE_INPUT = "input";

    public static final String COMPONENT_TYPE_PROCESSOR = "processor";

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

    @Inject
    private VirtualDependenciesService virtualDependenciesService;

    @Inject
    private ExtensionComponentMetadataManager virtualComponents;

    @Inject
    private MapCache caches;

    @Inject
    private SimpleQueryLanguageCompiler queryLanguageCompiler;

    @Inject
    @Context
    private HttpHeaders headers;

    @Inject
    private SecurityUtils secUtils;

    private String defaultTheme;

    private final Map<String, Function<ComponentIndex, Object>> componentEvaluators = new HashMap<>();

    @PostConstruct
    private void setupRuntime() {
        log.info("[setupRuntime] Initializing " + getClass());
        defaultTheme = configuration.getIconDefaultTheme();
        final String themeMode = configuration.getSupportIconTheme() ? "themed" : "legacy";
        log.info("[setupRuntime] Icon mode: {}; default theme: {}.", themeMode, defaultTheme);
        // preload some highly used data
        getIndex("en", false, null, defaultTheme);

        componentEvaluators.put("plugin", c -> c.getId().getPlugin());
        componentEvaluators.put("id", c -> c.getId().getId());
        componentEvaluators.put("familyId", c -> c.getId().getFamilyId());
        componentEvaluators.put("name", c -> c.getId().getName());
        componentEvaluators.put("metadata", component -> {
            final Iterator<SimplePropertyDefinition> iterator =
                    getDetail("en", new String[] { component.getId().getId() })
                            .getDetails()
                            .iterator()
                            .next()
                            .getProperties()
                            .iterator();
            if (iterator.hasNext()) {
                return iterator.next().getMetadata();
            }
            return emptyMap();
        });
    }

    public void clearCache(@Observes final DeployedComponent deployedComponent) {
        indicesPerRequest.clear();
    }

    @Override
    @CacheResult
    public Dependencies getDependencies(final String[] ids) {
        if (ids.length == 0) {
            return new Dependencies(emptyMap());
        }
        final Map<String, DependencyDefinition> dependencies = new HashMap<>();
        for (final String id : ids) {
            if (virtualComponents.isExtensionEntity(id)) {
                final DependencyDefinition deps = ofNullable(virtualComponents.getDependenciesFor(id))
                        .orElseGet(() -> new DependencyDefinition(emptyList()));
                dependencies.put(id, deps);
            } else {
                final ComponentFamilyMeta.BaseMeta<Lifecycle> meta = componentDao.findById(id);

                if (meta == null) {
                    // Manage when the meta is null because of an unknown identifier
                    throw new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(COMPONENT_MISSING,
                                    "No component matching the id: " + id))
                            .build());
                }

                dependencies.put(meta.getId(), getDependenciesFor(meta));
            }
        }
        return new Dependencies(dependencies);
    }

    @Override
    @CacheResult
    public StreamingOutput getDependency(final String id) {
        final ComponentFamilyMeta.BaseMeta<?> component = componentDao.findById(id);
        final Supplier<InputStream> streamProvider;
        if (component != null) { // local dep
            final Path file = componentManagerService
                    .manager()
                    .findPlugin(component.getParent().getPlugin())
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(PLUGIN_MISSING,
                                    "No plugin matching the id: " + id))
                            .build()))
                    .getContainerFile()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(PLUGIN_MISSING,
                                    "No dependency matching the id: " + id))
                            .build()));
            if (!Files.exists(file)) {
                return onMissingJar(id);
            }
            streamProvider = () -> {
                try {
                    return Files.newInputStream(file);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            };
        } else { // just try to resolve it locally, note we would need to ensure some security here
            final Artifact artifact = Artifact.from(id);
            if (virtualDependenciesService.isVirtual(id)) {
                streamProvider = virtualDependenciesService.retrieveArtifact(artifact);
                if (streamProvider == null) {
                    return onMissingJar(id);
                }
            } else {
                final Path file = componentManagerService.manager().getContainer().resolve(artifact.toPath());
                if (!Files.exists(file)) {
                    return onMissingJar(id);
                }
                streamProvider = () -> {
                    try {
                        return Files.newInputStream(file);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        }
        return output -> {
            final byte[] buffer = new byte[40960]; // 5k
            try (final InputStream stream = new BufferedInputStream(streamProvider.get(), buffer.length)) {
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

    @Override
    @CacheResult
    public ComponentIndices getIndex(final String language, final boolean includeIconContent, final String query,
            final String theme) {
        final Locale locale = localeMapper.mapLocale(language);
        final String themedIcon = theme == null ? defaultTheme : theme;
        caches.evictIfNeeded(indicesPerRequest, configuration.getMaxCacheSize() - 1);
        return indicesPerRequest.computeIfAbsent(new RequestKey(locale, includeIconContent, query, themedIcon), k -> {
            final Predicate<ComponentIndex> filter = queryLanguageCompiler.compile(query, componentEvaluators);
            return new ComponentIndices(Stream
                    .concat(findDeployedComponents(includeIconContent, locale, themedIcon), virtualComponents
                            .getDetails()
                            .stream()
                            .map(detail -> new ComponentIndex(
                                    detail.getId(),
                                    detail.getDisplayName(),
                                    detail.getId().getFamily(),
                                    detail.getType(),
                                    new Icon(detail.getIcon(), null, null, themedIcon),
                                    new Icon(virtualComponents.getFamilyIconFor(detail.getId().getFamilyId()), null,
                                            null, themedIcon),
                                    detail.getVersion(),
                                    singletonList(detail.getId().getFamily()),
                                    detail.getLinks(),
                                    detail.getMetadata())))
                    .filter(filter)
                    .collect(toList()));
        });
    }

    @Override
    @CacheResult
    public Response familyIcon(final String id, final String theme) {
        if (virtualComponents.isExtensionEntity(id)) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for family: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final ComponentFamilyMeta meta = componentFamilyDao.findById(id);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.FAMILY_MISSING, "No family for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
        final Optional<Container> plugin = manager.findPlugin(meta.getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(PLUGIN_MISSING,
                            "No plugin '" + meta.getPlugin() + "' for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get(), meta.getIcon(), theme);
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for family identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    @Override
    @CacheResult
    public Response icon(final String familyId, final String iconKey, final String theme) {
        if (virtualComponents.isExtensionEntity(familyId)) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for family: " + familyId))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final ComponentFamilyMeta meta = componentFamilyDao.findById(familyId);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.FAMILY_MISSING, "No family for identifier: " + familyId))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
        final Optional<Container> plugin = manager.findPlugin(meta.getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(PLUGIN_MISSING,
                            "No plugin '" + meta.getPlugin() + "' for family identifier: " + familyId))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get(), iconKey, theme);
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for icon key: " + iconKey))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    @Override
    @CacheResult
    public Response icon(final String id, final String theme) {
        if (virtualComponents.isExtensionEntity(id)) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for family: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final ComponentFamilyMeta.BaseMeta<Lifecycle> meta = componentDao.findById(id);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(COMPONENT_MISSING, "No component for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final Optional<Container> plugin = manager.findPlugin(meta.getParent().getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(PLUGIN_MISSING,
                            "No plugin '" + meta.getParent().getPlugin() + "' for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get(), meta.getIcon(), theme);
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    @Override
    @CacheResult
    public Response getIconIndex(final String theme) {
        final String themedIcon = theme == null ? defaultTheme : theme;
        try {
            final Map<String, IconSymbol> icons = collectIcons(themedIcon);
            if (icons.isEmpty()) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No svg icon available"))
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }
            // build the document.
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            final Element root = doc.createElement("svg");
            root.setAttribute("xmlns", "http://www.w3.org/2000/svg");
            root.setAttribute("focusable", "false");
            root.setAttribute("class", "sr-only");
            root.setAttribute("theme", themedIcon);
            doc.appendChild(root);
            icons.values().forEach(icon -> {
                final Element symbol = doc.createElement("symbol");
                symbol.setAttribute("family", icon.getFamily());
                symbol.setAttribute("id", icon.getIcon());
                symbol.setAttribute("type", icon.getType());
                symbol.setAttribute("connector", icon.getConnector());
                symbol.setAttribute("theme", icon.getTheme());
                symbol.setTextContent(new String(icon.getContent()));
                root.appendChild(symbol);
            });
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
            final Writer writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            final String svgs = writer.toString()
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">");

            return Response.ok(svgs).type(APPLICATION_SVG_XML_TYPE).build();
        } catch (Exception e) {
            log.error("[getIconIndex] {}", e.getMessage());
            return Response
                    .status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, e.getMessage()))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    @Override
    public Map<String, String> migrate(final String id, final int version, final Map<String, String> config) {
        final Map<String, String> configuration = config.entrySet().stream().map(e -> {
            if (e.getValue().startsWith(BASE64_PREFIX)) {
                final String value = new String(Base64
                        .getUrlDecoder()
                        .decode(e.getValue().substring(BASE64_PREFIX.length()).getBytes(StandardCharsets.UTF_8)));
                e.setValue(value);
            }
            return e;
        }).collect(toMap(Entry::getKey, Entry::getValue));
        if (virtualComponents.isExtensionEntity(id)) {
            return config;
        }
        final BaseMeta<Lifecycle> comp = ofNullable(componentDao.findById(id))
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Status.NOT_FOUND)
                        .entity(new ErrorPayload(COMPONENT_MISSING, "Didn't find component " + id))
                        .build()));
        if (version > comp.getVersion() || version == comp.getVersion()) {
            return config;
        }
        return ofNullable(componentDao.findById(id))
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ErrorPayload(COMPONENT_MISSING, "Didn't find component " + id))
                        .build()))
                .getMigrationHandler()
                .get()
                .migrate(version, config);
    }

    @Override
    @CacheResult
    public ComponentDetailList getDetail(final String language, final String[] ids) {
        if (ids == null || ids.length == 0) {
            return new ComponentDetailList(emptyList());
        }

        final Map<String, ErrorPayload> errors = new HashMap<>();
        final List<ComponentDetail> details = Stream.of(ids).map(id -> {
            if (virtualComponents.isExtensionEntity(id)) {
                return virtualComponents.findComponentById(id).orElseGet(() -> {
                    errors.put(id, new ErrorPayload(COMPONENT_MISSING, "No virtual component '" + id + "'"));
                    return null;
                });
            }
            return ofNullable(componentDao.findById(id)).map(meta -> {
                final Optional<Container> plugin = manager.findPlugin(meta.getParent().getPlugin());
                if (!plugin.isPresent()) {
                    errors
                            .put(meta.getId(), new ErrorPayload(PLUGIN_MISSING,
                                    "No plugin '" + meta.getParent().getPlugin() + "'"));
                    return null;
                }

                final Container container = plugin.get();
                final Optional<DesignModel> model = ofNullable(meta.get(DesignModel.class));
                if (!model.isPresent()) {
                    errors
                            .put(meta.getId(),
                                    new ErrorPayload(DESIGN_MODEL_MISSING, "No design model '" + meta.getId() + "'"));
                    return null;
                }

                final Locale locale = localeMapper.mapLocale(language);
                final String type;
                if (ProcessorMeta.class.isInstance(meta)) {
                    type = COMPONENT_TYPE_PROCESSOR;
                } else if (PartitionMapperMeta.class.isInstance(meta)) {
                    type = COMPONENT_TYPE_INPUT;
                } else {
                    type = COMPONENT_TYPE_STANDALONE;
                }

                final ComponentBundle bundle = meta.findBundle(container.getLoader(), locale);
                final ComponentDetail componentDetail = new ComponentDetail();
                componentDetail.setLinks(emptyList());
                componentDetail.setId(createMetaId(container, meta));
                componentDetail.setVersion(meta.getVersion());
                componentDetail.setIcon(meta.getIcon());
                componentDetail.setInputFlows(model.get().getInputFlows());
                componentDetail.setOutputFlows(model.get().getOutputFlows());
                componentDetail.setType(type);
                componentDetail.setDisplayName(bundle.displayName().orElse(meta.getName()));
                componentDetail.setProperties(propertiesService
                        .buildProperties(meta.getParameterMetas().get(), container.getLoader(), locale, null)
                        .collect(toList()));
                componentDetail.setActions(actionsService
                        .findActions(meta.getParent().getName(), container, locale, meta,
                                meta.getParent().findBundle(container.getLoader(), locale)));
                componentDetail.setMetadata(translateMetadata(meta.getMetadata(), bundle));

                return componentDetail;
            }).orElseGet(() -> {
                errors.put(id, new ErrorPayload(COMPONENT_MISSING, "No component '" + id + "'"));
                return null;
            });
        }).filter(Objects::nonNull).collect(toList());

        if (!errors.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
        }

        return new ComponentDetailList(details);
    }

    private Map<String, IconSymbol> collectIcons(final String theme) {
        if ("all".equals(theme)) {
            Map<String, IconSymbol> icons = getAllIconsForTheme("light");
            icons.putAll(getAllIconsForTheme("dark"));
            return icons;
        } else {
            return getAllIconsForTheme(theme);
        }
    }

    private Map<String, IconSymbol> getAllIconsForTheme(final String theme) {
        final ComponentIndices index = getIndex(Locale.ROOT.getLanguage(), true, null, theme);
        try {
            final List<ComponentIndex> components = index.getComponents();
            Map<String, IconSymbol> icons = components
                    .stream()
                    .filter(c -> c.getIconFamily().getCustomIcon() != null)
                    .filter(c -> "image/svg+xml".equals(c.getIconFamily().getCustomIconType()))
                    .map(c -> new IconSymbol(c.getIconFamily().getIcon(),
                            c.getFamilyDisplayName(),
                            "family",
                            "",
                            theme,
                            c.getIconFamily().getCustomIcon()))
                    .collect(toMap(IconSymbol::getUid, identity(), (r1, r2) -> r1));
            icons.putAll(components
                    .stream()
                    .filter(c -> c.getIcon().getCustomIcon() != null)
                    .filter(c -> c.getIcon().getCustomIcon() != null)
                    .filter(c -> "image/svg+xml".equals(c.getIcon().getCustomIconType()))
                    .map(c -> new IconSymbol(c.getIcon().getIcon(),
                            c.getFamilyDisplayName(),
                            "connector",
                            c.getDisplayName(),
                            theme,
                            c.getIcon().getCustomIcon()))
                    .collect(toMap(IconSymbol::getUid, identity(), (r1, r2) -> r1)));
            return icons;
        } catch (Exception e) {
            throw e;
        }
    }

    private Stream<ComponentIndex> findDeployedComponents(final boolean includeIconContent, final Locale locale,
            final String theme) {
        return manager
                .find(c -> c
                        .execute(() -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                        .flatMap(component -> Stream
                                .of(component
                                        .getPartitionMappers()
                                        .values()
                                        .stream()
                                        .map(mapper -> toComponentIndex(c, locale, c.getId(), mapper,
                                                c.get(ComponentManager.OriginalId.class), includeIconContent,
                                                COMPONENT_TYPE_INPUT, theme)),
                                        component
                                                .getProcessors()
                                                .values()
                                                .stream()
                                                .map(proc -> toComponentIndex(c, locale, c.getId(), proc,
                                                        c.get(ComponentManager.OriginalId.class), includeIconContent,
                                                        COMPONENT_TYPE_PROCESSOR, theme)),
                                        component
                                                .getDriverRunners()
                                                .values()
                                                .stream()
                                                .map(runner -> toComponentIndex(c, locale, c.getId(), runner,
                                                        c.get(ComponentManager.OriginalId.class), includeIconContent,
                                                        COMPONENT_TYPE_STANDALONE, theme)))
                                .flatMap(Function.identity())));
    }

    private DependencyDefinition getDependenciesFor(final ComponentFamilyMeta.BaseMeta<?> meta) {
        final ComponentFamilyMeta familyMeta = meta.getParent();
        final Optional<Container> container = componentManagerService.manager().findPlugin(familyMeta.getPlugin());
        return new DependencyDefinition(container.map(c -> {
            final ComponentExtension.ComponentContext context =
                    c.get(ComponentContexts.class).getContexts().get(meta.getType());
            final ComponentExtension extension = context.owningExtension();
            final Stream<Artifact> deps = c.findDependencies();
            final Stream<Artifact> artifacts;
            if (configuration.getAddExtensionDependencies() && extension != null) {
                final List<Artifact> dependencies = deps.collect(toList());
                final Stream<Artifact> addDeps = getExtensionDependencies(extension, dependencies);
                artifacts = Stream.concat(dependencies.stream(), addDeps);
            } else {
                artifacts = deps;
            }
            return artifacts.map(Artifact::toCoordinate).collect(toList());
        }).orElseThrow(() -> new IllegalArgumentException("Can't find container '" + meta.getId() + "'")));
    }

    private Stream<Artifact> getExtensionDependencies(final ComponentExtension extension,
            final List<Artifact> filtered) {
        return extension
                .getAdditionalDependencies()
                .stream()
                .map(Artifact::from)
                // filter required artifacts if they are already present in the list.
                .filter(extArtifact -> filtered
                        .stream()
                        .map(d -> d.getGroup() + ":" + d.getArtifact())
                        .noneMatch(ga -> ga.equals(extArtifact.getGroup() + ":" + extArtifact.getArtifact())));
    }

    private ComponentId createMetaId(final Container container, final ComponentFamilyMeta.BaseMeta<Lifecycle> meta) {
        return new ComponentId(meta.getId(), meta.getParent().getId(), meta.getParent().getPlugin(),
                ofNullable(container.get(ComponentManager.OriginalId.class))
                        .map(ComponentManager.OriginalId::getValue)
                        .orElse(container.getId()),
                meta.getParent().getName(), meta.getName());
    }

    private ComponentIndex toComponentIndex(final Container container, final Locale locale, final String plugin,
            final ComponentFamilyMeta.BaseMeta meta, final ComponentManager.OriginalId originalId,
            final boolean includeIcon, final String type, final String theme) {
        final ClassLoader loader = container.getLoader();
        final String iconTheme = theme == null ? defaultTheme : theme;
        final String icon = meta.getIcon();
        final String familyIcon = meta.getParent().getIcon();
        final IconResolver.Icon iconContent = iconResolver.resolve(container, icon, iconTheme);
        final IconResolver.Icon iconFamilyContent = iconResolver.resolve(container, familyIcon, iconTheme);
        final FamilyBundle parentBundle = meta.getParent().findBundle(loader, locale);
        final ComponentBundle bundle = meta.findBundle(loader, locale);
        final String familyDisplayName = parentBundle.displayName().orElse(meta.getParent().getName());
        final List<String> categories = ofNullable(meta.getParent().getCategories())
                .map(vals -> vals
                        .stream()
                        .map(this::normalizeCategory)
                        .map(category -> category.replace("${family}", meta.getParent().getName()))
                        .map(category -> parentBundle.category(category)
                                .orElseGet(() -> category.replace("/" + meta.getParent().getName() + "/",
                                        "/" + familyDisplayName + "/")))
                        .collect(toList()))
                .orElseGet(Collections::emptyList);
        return new ComponentIndex(
                new ComponentId(meta.getId(), meta.getParent().getId(), plugin,
                        ofNullable(originalId).map(ComponentManager.OriginalId::getValue).orElse(plugin),
                        meta.getParent().getName(), meta.getName()),
                bundle.displayName().orElse(meta.getName()),
                familyDisplayName, type,
                new Icon(icon, iconContent == null ? null : iconContent.getType(),
                        !includeIcon ? null : (iconContent == null ? null : iconContent.getBytes()), iconTheme),
                new Icon(familyIcon, iconFamilyContent == null ? null : iconFamilyContent.getType(),
                        !includeIcon ? null : (iconFamilyContent == null ? null : iconFamilyContent.getBytes()),
                        iconTheme),
                meta.getVersion(),
                categories,
                singletonList(new Link("Detail", "/component/details?identifiers=" + meta.getId(),
                        MediaType.APPLICATION_JSON)),
                //
                translateMetadata(meta.getMetadata(), bundle));
    }

    private String normalizeCategory(final String category) {
        // we prevent root categories and always append the family in this case
        if (!category.contains("${family}")) {
            return category + "/${family}";
        }
        return category;
    }

    private Map<String, String> translateMetadata(final Map<String, String> source, final ComponentBundle bundle) {
        return source
                .entrySet()
                .stream()
                .map(e -> bundle.displayName(e.getKey().replaceAll("::", "."))
                        .map(it -> (Entry<String, String>) new SimpleEntry(e.getKey(), it))
                        .orElse(e))
                .map(t -> {
                    if (t.getKey().equals("documentation::value")) {
                        final String bundleDoc = bundle.documentation().orElse(null);
                        if (bundleDoc != null) {
                            t.setValue(bundleDoc);
                        }
                    }
                    return t;
                })
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    private StreamingOutput onMissingJar(final String id) {
        throw new WebApplicationException(Response
                .status(Response.Status.NOT_FOUND)
                .type(APPLICATION_JSON_TYPE)
                .entity(new ErrorPayload(PLUGIN_MISSING, "No file found for: " + id))
                .build());
    }
}
