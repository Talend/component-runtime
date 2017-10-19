// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.maven;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.components.container.Container;
import org.talend.components.runtime.manager.ComponentManager;
import org.talend.components.runtime.manager.ContainerComponentRegistry;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Will generate a TALEND-INF/components.json with the list of components and metadata.
 */
// TBD: do we want to unify it with the rest layer? today it is highly coupled with the studio and lighter in the sense
// it doesn't handle properties since studio will then load the plugin to handle them but that's an option if desired
@Mojo(name = "metadata", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class ComponentMetadataMojo extends ComponentManagerBasedMojo {

    @Parameter(defaultValue = "TALEND-INF/components.json", property = "talend.metadata.location")
    private String location;

    @Override
    protected void doWork(final ComponentManager manager, final Container container, final ContainerComponentRegistry registry)
            throws MojoExecutionException, MojoFailureException {
        final File output = new File(classes, location);
        if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            throw new MojoExecutionException("Can't create " + output);
        }

        final Collection<Component> components = registry.getComponents().values().stream()
                .flatMap(
                        c -> Stream.concat(
                                c.getPartitionMappers().values().stream()
                                        .map(p -> new Component(p.getParent().getCategories(), p.getParent().getName(),
                                                p.getName(),
                                                p.findBundle(container.getLoader(), Locale.ENGLISH).displayName()
                                                        .orElse(p.getName()),
                                                p.getIcon(), emptyList(), singletonList("MAIN"))),
                                c.getProcessors().values().stream().map(p -> {
                                    final Method listener = findListener(p);
                                    return new Component(p.getParent().getCategories(), p.getParent().getName(), p.getName(),
                                            p.findBundle(container.getLoader(), Locale.ENGLISH).displayName().orElse(p.getName()),
                                            p.getIcon(), buildInputs(listener), buildOutputs(listener));
                                })))
                .collect(toList());

        try (final Jsonb mapper = inPluginContext(JsonbBuilder::newBuilder).withConfig(new JsonbConfig()
                .setProperty("johnzon.cdi.activated", false).setProperty("johnzon.attributeOrder", String.CASE_INSENSITIVE_ORDER))
                .build()) {
            container.execute(() -> {
                try {
                    mapper.toJson(new ComponentContainer(components), new FileOutputStream(output));
                } catch (final FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }

                getLog().info("Created " + output);
                return null;
            });
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    public static class ComponentContainer {

        private Collection<Component> components;
    }

    @Data
    @AllArgsConstructor
    public static class Component {

        private Collection<String> categories;

        private String family;

        private String name;

        private String displayName;

        private String icon;

        private Collection<String> inputs;

        private Collection<String> outputs;
    }
}
