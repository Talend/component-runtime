/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.server.extension.api.ExtensionRegistrar;
import org.talend.sdk.component.server.extension.api.action.Action;
import org.talend.sdk.component.server.extension.stitch.model.Model;
import org.talend.sdk.component.server.extension.stitch.runtime.StitchGenericComponent;
import org.talend.sdk.component.server.extension.stitch.runtime.StitchInput;
import org.talend.sdk.component.server.extension.stitch.runtime.StitchMapper;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.spi.component.GenericComponentExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class StitchProvisioning {

    @Inject
    private ActionHandler actionHandler;

    void register(@ObservesAsync final ExtensionRegistrar event, final StitchConfiguration configuration,
            @Ext final WebTarget target) {
        if (!configuration.getToken().isPresent()) {
            log.info("Skipping stitch extension since token is not set");
            return;
        }

        // ensure we can create our runtime first
        final DependencyDefinition dependencyDefinition = getRuntimeDependency(event);

        log.info("Loading stitch data, this is an experimental extension");

        final CountDownLatch latch = new CountDownLatch(1);
        event.registerAwait(() -> {
            try {
                latch.await(configuration.getInitTimeout(), MILLISECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        new ModelLoader(target, configuration.getToken().orElseThrow(IllegalArgumentException::new),
                configuration.getRetries()).load().thenApply(model -> {
                    final Map<String, DependencyDefinition> dependenciesMap =
                            createDependenciesMap(dependencyDefinition, model);

                    final List<ActionReference> actions = model
                            .getConfigurations()
                            .stream()
                            .filter(this::isDataset)
                            .peek(config -> config.setActions(extractActions(config.getProperties()).collect(toList())))
                            .map(ConfigTypeNode::getActions)
                            .flatMap(Collection::stream)
                            .distinct()
                            .collect(toList());

                    final List<Action> actionWithHandler = actions
                            .stream()
                            .map(ref -> new Action(ref, (payload, lang) -> actionHandler.onAction(ref, payload, lang)))
                            .collect(toList());

                    model
                            .getComponents()
                            .forEach(component -> component
                                    .setActions(extractActions(component.getProperties()).collect(toList())));

                    event.registerComponents(model.getComponents());
                    event.registerConfigurations(model.getConfigurations());
                    event.registerDependencies(dependenciesMap);
                    event.registerActions(actionWithHandler);
                    return model;
                }).whenComplete((model, error) -> {
                    if (error != null) {
                        log.error(error.getMessage(), error);
                    } else {
                        log.info("Loaded {} Stitch components", model.getComponents().size());
                    }
                    latch.countDown();
                });
    }

    private boolean isDataset(final ConfigTypeNode it) {
        return "dataset".equalsIgnoreCase(it.getConfigurationType());
    }

    // only datasets have actions and it is only schema and field suggestions so impl is biased
    private Stream<ActionReference> extractActions(final Collection<SimplePropertyDefinition> props) {
        return props.stream().filter(it -> it.getMetadata().containsKey("action::suggestions")).map(it -> {
            final String name = it.getMetadata().get("action::suggestions");
            if (name.startsWith("fields_")) {
                return new ActionReference("Stitch", name, "suggestions", name,
                        Stream.concat(findStepForm(props).stream(), findSchema(props).stream()).collect(toList()));
            } else if (name.startsWith("schema_")) { // resolve steps_form
                return new ActionReference("Stitch", name, "suggestions", name, findStepForm(props));
            } else {
                throw new IllegalArgumentException("Unknown trigger: " + name);
            }
        });
    }

    private List<SimplePropertyDefinition> findStepForm(final Collection<SimplePropertyDefinition> props) {
        return findParam(props, "step_form", "OBJECT", 0);
    }

    private List<SimplePropertyDefinition> findSchema(final Collection<SimplePropertyDefinition> props) {
        return findParam(props, "schema", "ARRAY", 1);
    }

    private List<SimplePropertyDefinition> findParam(final Collection<SimplePropertyDefinition> props,
            final String name, final String type, final int index) {
        return props
                .stream()
                .filter(p -> name.equals(p.getName()) && type.equalsIgnoreCase(p.getType()))
                .findFirst()
                .map(root -> props.stream().filter(p -> p.getPath().startsWith(root.getPath())).map(p -> {
                    final String newPath = name + p.getPath().substring(root.getPath().length());
                    if (newPath.equals(name)) {
                        final Map<String, String> metadata = new HashMap<>(p.getMetadata());
                        metadata.put("definition::parameter::index", Integer.toString(index));
                        return new SimplePropertyDefinition(newPath, p.getName(), p.getDisplayName(), p.getType(),
                                p.getDefaultValue(), p.getValidation(), metadata, p.getPlaceholder(),
                                p.getProposalDisplayNames());
                    }
                    return new SimplePropertyDefinition(newPath, p.getName(), p.getDisplayName(), p.getType(),
                            p.getDefaultValue(), p.getValidation(), p.getMetadata(), p.getPlaceholder(),
                            p.getProposalDisplayNames());
                }).collect(toList()))
                .orElseThrow(() -> new IllegalArgumentException("Can't find " + name + " in " + props));
    }

    private Map<String, DependencyDefinition> createDependenciesMap(final DependencyDefinition dependencyDefinition,
            final Model model) {
        return model.getComponents().stream().collect(toMap(it -> it.getId().getId(), it -> dependencyDefinition));
    }

    private DependencyDefinition getRuntimeDependency(final ExtensionRegistrar event) {
        final String groupId = "org.talend.stitch.generated";
        final String artifactId = "stitch"; // match the family for later lookups

        // for tests
        final String versionMarker = System.getProperty("talend.component.server.extension.stitch.versionMarker", "");
        final String version = "1.0.0" + (versionMarker.isEmpty() ? "" : ('-' + versionMarker));

        event.createExtensionJarIfNotExist(groupId, artifactId, version, jar -> {
            try {
                {
                    // write manifest.mf
                    final Manifest manifest = new Manifest();
                    final Attributes mainAttributes = manifest.getMainAttributes();
                    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
                    mainAttributes.putValue("Created-By", "Talend Component Kit Server Stitch Extension");
                    mainAttributes.putValue("Talend-Time", Long.toString(System.currentTimeMillis()));
                    mainAttributes.putValue("Talend-Family-Name", "Stitch");

                    final ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
                    jar.putNextEntry(e);
                    manifest.write(new BufferedOutputStream(jar));
                    jar.closeEntry();
                }

                // create root package (folders)
                final StringBuilder pck = new StringBuilder();
                for (final String s : StitchGenericComponent.class.getPackage().getName().split("\\.")) {
                    pck.append(s).append('/');
                    jar.putNextEntry(new JarEntry(pck.toString()));
                    jar.closeEntry();
                }

                // now we add our classes in that package
                Stream
                        .of(StitchGenericComponent.class, StitchMapper.class, StitchInput.class,
                                org.talend.sdk.component.server.extension.stitch.runtime.StitchClient.class)
                        .forEach(clazz -> {
                            final String resource = clazz.getName().replace('.', '/') + ".class";
                            try (final InputStream stream =
                                    StitchProvisioning.class.getClassLoader().getResourceAsStream(resource)) {
                                if (stream == null) {
                                    throw new IllegalStateException("Can't find " + resource);
                                }

                                jar.putNextEntry(new JarEntry(resource));
                                final byte[] buffer = new byte[8192];
                                int read;
                                while ((read = stream.read(buffer)) >= 0) {
                                    jar.write(buffer, 0, read);
                                }
                                jar.closeEntry();
                            } catch (final IOException streamError) {
                                throw new IllegalStateException(streamError);
                            }
                        });
                // register our generic component
                jar.putNextEntry(new JarEntry("META-INF/services/" + GenericComponentExtension.class.getName()));
                jar.write((StitchGenericComponent.class.getName()).getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            } catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        });
        return new DependencyDefinition(singletonList(groupId + ':' + artifactId + ':' + version));
    }
}
