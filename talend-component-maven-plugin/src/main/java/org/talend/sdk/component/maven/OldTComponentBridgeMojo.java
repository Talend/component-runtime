/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.apache.maven.shared.utils.StringUtils.capitalise;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.IOUtil;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.ServiceMeta;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ActionParameterEnricher;
import org.talend.sdk.component.runtime.output.Branches;

import lombok.Data;

// todo: move it to an utility able to work from a .jar?
// todo: review the ComponentManager usage since the shutdown hook shouldnt be used
@Mojo(name = "prepare", defaultPhase = PACKAGE, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class OldTComponentBridgeMojo extends ComponentManagerBasedMojo {

    private static final String ROOT_PROPERTIES = "rootProperties.";

    @Parameter(defaultValue = "true", property = "talend.component.attach")
    private boolean attach;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File target;

    @Parameter(defaultValue = "${project.groupId}", readonly = true)
    private String groupId;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String version;

    @Parameter(defaultValue = "https://raw.githubusercontent.com/Talend/ui/master/packages/icons/src/svg/", property = "talend.component.svgRrepository")
    private String svgRepository;

    @Parameter(defaultValue = "false", property = "talend.component.downloadIconsFromGithub")
    private boolean downloadIconsFromGithub;

    @Parameter(defaultValue = "auto", property = "talend.component.componentsApiVersion")
    private String componentsApiVersion;

    @Parameter(defaultValue = "auto", property = "talend.component.componentNgApiVersion")
    private String componentNgApiVersion;

    @Parameter(property = "talend.component.studioHome")
    private File studioHome;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    private final Freemarkers freemarkers = new Freemarkers(getClass().getSimpleName());

    @Override
    protected void doWork(final ComponentManager manager, final Container container, final ContainerComponentRegistry registry)
            throws MojoExecutionException, MojoFailureException {
        createComponentModules(container, registry);
    }

    @Override
    protected void pluginInit() throws MojoExecutionException {
        if ("auto".equals(componentsApiVersion) || null == componentsApiVersion) {
            try (final InputStream stream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("META-INF/maven/org.talend.components/components-api/pom.properties")) {
                final Properties properties = new Properties();
                properties.load(stream);
                componentsApiVersion = properties.getProperty("version", "0.19.7");
            } catch (final IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        if ("auto".equals(componentNgApiVersion) || null == componentNgApiVersion) {
            try (final InputStream stream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("META-INF/maven/org.talend.sdk.component/component-api/pom.properties")) {
                final Properties properties = new Properties();
                properties.load(stream);
                componentNgApiVersion = properties.getProperty("version", "1.0.0-SNAPSHOT");
            } catch (final IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    private void createComponentModules(final Container container, final ContainerComponentRegistry registry)
            throws MojoExecutionException {
        if (!classes.exists()) {
            throw new MojoExecutionException("No " + classes + " so no component available");
        }

        final File workDir = new File(target, getClass().getSimpleName().toLowerCase(ENGLISH) + "_workdir");
        mkdirP(workDir);

        final Collection<File> modules = new ArrayList<>();

        final File fakeM2 = new File(workDir, ".m2");
        final File artifactM2Dir = new File(fakeM2, groupId.replace('.', '/') + '/' + artifactId + '/' + version);
        mkdirP(artifactM2Dir);
        try (final InputStream jar = new FileInputStream(project.getArtifact().getFile());
                final OutputStream out = new FileOutputStream(
                        new File(artifactM2Dir, project.getBuild().getFinalName() + '.' + packaging))) {
            IOUtil.copy(jar, out);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        try (final InputStream pom = new FileInputStream(new File(project.getBasedir(), "pom.xml"));
                final OutputStream out = new FileOutputStream(
                        new File(artifactM2Dir, project.getBuild().getFinalName() + ".pom"))) {
            IOUtil.copy(pom, out);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        registry.getComponents().values().forEach(family -> {
            if (artifactId.equals(family.getName())) {
                throw new IllegalArgumentException(
                        "You can't name your artifact with a family name (" + family + "), please rename it");
            }

            final String artifactBaseName = family.getName().toLowerCase(ENGLISH);
            final File rootFolder = new File(workDir, artifactBaseName);
            final File definitionRootFolder = new File(rootFolder, artifactBaseName + "-definition");
            mkdirP(definitionRootFolder);
            final File runtimeRootFolder = new File(rootFolder, artifactBaseName + "-runtime");
            mkdirP(runtimeRootFolder);

            final Collection<Definition> definitions = new ArrayList<>();

            final Collection<String> packages = new ArrayList<>();

            family.getPartitionMappers().values().forEach(mapper -> {
                final Class<?> type = mapper.getType();
                final String baseName = type.getSimpleName();
                final String definitionName = baseName + "Definition";
                final String propertyName = baseName + "Properties";
                final String runtimeName = baseName + "Source";
                final String runtimePackage = type.getPackage().getName().replace('.', '/') + "/runtime";
                final String definitionPackage = type.getPackage().getName().replace('.', '/') + "/definition";

                final byte[] iconContent = Icons.findIcon(mapper.getIcon(), downloadIconsFromGithub, svgRepository);
                final String packageForJava = definitionPackage.replace('/', '.');
                final Definition definition = new Definition(definitionName, packageForJava, mapper.getParent().getName(),
                        mapper.getName(),
                        ofNullable(mapper.getParent().getCategories()).orElseGet(Collections::emptyList).stream()
                                .map(c -> c + '/' + mapper.getParent().getName()).collect(toList()),
                        packageForJava + '.' + propertyName, runtimePackage.replace('/', '.') + '.' + runtimeName,
                        singleton("OUTGOING"), mapper.getIcon(), iconContent != null);
                definitions.add(definition);

                final File definitionFile = new File(definitionRootFolder,
                        "src/main/java/" + definitionPackage + '/' + definitionName + ".java");
                packages.add(packageForJava);
                doWrite(definitionFile.getParentFile(), definitionFile.getName(),
                        freemarkers.templatize("definition", new HashMap<String, Object>() {

                            {
                                put("generatedGroupId", groupId);
                                put("generatedVersion", version);
                                put("runtimeArtifactId", runtimeRootFolder.getName());
                                put("definition", definition);
                            }
                        }));

                // properties
                final Map<String, ParameterValue> parametersMap = buildParametersMap(ROOT_PROPERTIES, mapper.getParameterMetas(),
                        emptyList());
                final ServiceMeta.ActionMeta guessSchema = hasGuessSchemaAction(registry, mapper);
                final String datasetPrefix = findDatasetPrefix(mapper, guessSchema);
                generateProperties(container.getLoader(), definitionFile.getParentFile(), propertyName, definition,
                        mapper.getParameterMetas(), true, guessSchema, () -> generateGuessSchema(family, mapper, parametersMap,
                                runtimeRootFolder.getName(), guessSchema, datasetPrefix, null, null, null),
                        false, null);

                // icon
                if (iconContent != null) {
                    final File iconFile = new File(definitionRootFolder,
                            "src/main/resources/" + packageForJava.replace('.', '/') + "/" + mapper.getIcon() + "_icon32.png");
                    mkdirP(iconFile.getParentFile());
                    try (final OutputStream out = new FileOutputStream(iconFile)) {
                        IOUtil.copy(iconContent, out);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                // i18n
                generateI18n(container, definitionRootFolder, mapper, packageForJava);

                // runtime
                doWrite(new File(runtimeRootFolder, "src/main/java/" + runtimePackage), runtimeName + ".java",
                        freemarkers.templatize("source", new HashMap<String, Object>() {

                            {
                                put("plugin", groupId + ':' + artifactId + ':' + version);
                                put("family", mapper.getParent().getName());
                                put("name", mapper.getName());
                                put("version", mapper.getVersion());
                                put("propertiesType", definition.propertiesClassName);
                                put("shortPropertiesType", definition.propertiesClassName
                                        .substring(definition.propertiesClassName.lastIndexOf('.') + 1));
                                put("parameters", parametersMap);
                                put("definition", new Definition(runtimeName, runtimePackage.replace('/', '.'), null, null, null,
                                        null, null, null, null, false));
                            }
                        }));
            });
            family.getProcessors().values().forEach(processor -> {
                final Class<?> type = processor.getType();
                final String baseName = type.getSimpleName();
                final String definitionName = baseName + "Definition";
                final String propertyName = baseName + "Properties";
                final String runtimeName = baseName + "Sink";
                final String definitionPackage = type.getPackage().getName().replace('.', '/') + "/definition";
                final String runtimePackage = type.getPackage().getName().replace('.', '/') + "/runtime";

                final byte[] iconContent = Icons.findIcon(processor.getIcon(), downloadIconsFromGithub, svgRepository);
                final String packageForJava = definitionPackage.replace('/', '.');
                final boolean hasOutput = hasOutput(processor);
                final Definition definition = new Definition(definitionName, packageForJava, processor.getParent().getName(),
                        processor.getName(),
                        ofNullable(processor.getParent().getCategories()).orElseGet(Collections::emptyList).stream()
                                .map(c -> c + '/' + processor.getParent().getName()).collect(toList()),
                        packageForJava + '.' + propertyName, runtimePackage.replace('/', '.') + '.' + runtimeName,
                        hasOutput ? singleton("INCOMING_AND_OUTGOING") : singleton("INCOMING"), processor.getIcon(),
                        iconContent != null);
                definitions.add(definition);

                final File definitionFile = new File(definitionRootFolder,
                        "src/main/java/" + definitionPackage + '/' + definitionName + ".java");
                packages.add(packageForJava);
                doWrite(definitionFile.getParentFile(), definitionFile.getName(),
                        freemarkers.templatize("definition", new HashMap<String, Object>() {

                            {
                                put("generatedGroupId", groupId);
                                put("generatedVersion", version);
                                put("runtimeArtifactId", runtimeRootFolder.getName());
                                put("definition", definition);
                            }
                        }));

                // properties
                final Method processorListener = findListener(processor);
                final Collection<String> inputs = buildInputs(processorListener);
                inputs.remove(Branches.DEFAULT_BRANCH);
                if (!inputs.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Only default branch is supported as @Input in @Processors, please fix " + processor);
                }
                final Class<?> recordType = findInputRecordType(processor, processorListener);

                final boolean processorHasOutput = hasOutput(processor);
                final Map<String, ParameterValue> parametersMap = buildParametersMap(ROOT_PROPERTIES,
                        processor.getParameterMetas(), emptyList());
                final ServiceMeta.ActionMeta guessSchema = hasGuessSchemaAction(registry, processor);
                final String datasetPrefix = findDatasetPrefix(processor, guessSchema);
                generateProperties(container.getLoader(), definitionFile.getParentFile(), propertyName, definition,
                        processor.getParameterMetas(), processorHasOutput, guessSchema,
                        () -> generateGuessSchema(family, processor, parametersMap, runtimeRootFolder.getName(), guessSchema,
                                datasetPrefix, recordType, findOutputRecordType(processorListener),
                                runtimePackage.replace('/', '.') + '.' + runtimeName),
                        true, null);

                // icon
                if (iconContent != null) {
                    final File iconFile = new File(definitionRootFolder,
                            "src/main/resources/" + packageForJava.replace('.', '/') + "/" + processor.getIcon() + "_icon32.png");
                    mkdirP(iconFile.getParentFile());
                    try (final OutputStream out = new FileOutputStream(iconFile)) {
                        IOUtil.copy(iconContent, out);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                // i18n
                generateI18n(container, definitionRootFolder, processor, packageForJava);

                // runtime
                doWrite(new File(runtimeRootFolder, "src/main/java/" + runtimePackage), runtimeName + ".java",
                        freemarkers.templatize("sink", new HashMap<String, Object>() {

                            {
                                put("plugin", groupId + ':' + artifactId + ':' + version);
                                put("family", processor.getParent().getName());
                                put("name", processor.getName());
                                put("version", processor.getVersion());
                                put("propertiesType", definition.propertiesClassName);
                                put("shortPropertiesType", definition.propertiesClassName
                                        .substring(definition.propertiesClassName.lastIndexOf('.') + 1));
                                put("parameters", parametersMap);
                                put("definition", new Definition(baseName, runtimePackage.replace('/', '.'), null, null, null,
                                        null, null, null, null, false));
                                put("recordType", recordType.getName());
                            }
                        }));
            });

            if (!definitions.isEmpty()) { // family

                final String rootPackage = findCommon(packages);

                final String name = capitalise(family.getName()) + "FamilyDefinition";

                doWrite(new File(definitionRootFolder, "src/main/java/" + rootPackage.replace('.', '/')), name + ".java",
                        freemarkers.templatize("familydefinition", new HashMap<String, Object>() {

                            {
                                put("family", family);
                                put("definition", new Definition(name, rootPackage, null, family.getName(), null, null, null,
                                        null, null, false));
                                put("definitions", definitions);
                            }
                        }));

                // pom.xml
                createPoms(fakeM2, family, rootFolder, definitionRootFolder, runtimeRootFolder, rootPackage);

                // ensure we register the ComponentInstaller
                final File servicesFolder = new File(definitionRootFolder, "src/main/resources/META-INF/services");
                doWrite(servicesFolder, "org.talend.components.api.ComponentInstaller", rootPackage + '.' + name);

                modules.add(rootFolder);
            }

            // runtime pom etc
        });

        doAttach(modules);
    }

    private String findDatasetPrefix(final ComponentFamilyMeta.BaseMeta<?> mapper, final ServiceMeta.ActionMeta guessSchema) {
        return guessSchema == null ? null
                : Stream.concat(mapper.getParameterMetas().stream(),
                        mapper.getParameterMetas().stream().flatMap(p -> p.getNestedParameters().stream()))
                        .filter(p -> guessSchema.getAction().equals(p.getMetadata().get("dataset"))).findFirst()
                        .map(ParameterMeta::getPath).orElse(null);
    }

    private Class<?> findInputRecordType(final ComponentFamilyMeta.ProcessorMeta processor, final Method processorListener) {
        return findInputs(processorListener).sorted(new Comparator<java.lang.reflect.Parameter>() {

            @Override
            public int compare(final java.lang.reflect.Parameter o1, final java.lang.reflect.Parameter o2) {
                return sortBranches(getBranch(o1), getBranch(o2));
            }

            private String getBranch(final java.lang.reflect.Parameter param) {
                return ofNullable(param.getAnnotation(Input.class)).map(Input::value).orElse(Branches.DEFAULT_BRANCH);
            }
        }).findFirst().map(java.lang.reflect.Parameter::getType)
                .orElseThrow(() -> new IllegalArgumentException("No input record for " + processor));
    }

    private Class<?> findOutputRecordType(final Method processorListener) {

        final Class<?> returnType = processorListener.getReturnType();
        return returnType == void.class || returnType == Void.class ? Stream.of(processorListener.getParameters())
                .filter(p -> p.isAnnotationPresent(Output.class)).sorted(new Comparator<java.lang.reflect.Parameter>() {

                    @Override
                    public int compare(final java.lang.reflect.Parameter o1, final java.lang.reflect.Parameter o2) {
                        return sortBranches(getBranch(o1), getBranch(o2));
                    }

                    private String getBranch(final java.lang.reflect.Parameter param) {
                        return ofNullable(param.getAnnotation(Output.class)).map(Output::value).orElse(Branches.DEFAULT_BRANCH);
                    }
                }).findFirst().map(java.lang.reflect.Parameter::getType).orElse(null) : returnType;
    }

    private int sortBranches(final String b1, final String b2) {
        if (b1.equals(b2)) { // weird processor but we can't really forbid it here
            return 0;
        }
        // main main prioritized (and the only supported branch with reject)
        if (Branches.DEFAULT_BRANCH.equals(b1)) {
            return -1;
        }
        if (Branches.DEFAULT_BRANCH.equals(b2)) {
            return 1;
        }
        return b1.compareTo(b2);
    }

    private void createPoms(final File fakeM2, final ComponentFamilyMeta family, final File rootFolder,
            final File definitionRootFolder, final File runtimeRootFolder, final String rootPackage) {
        doWrite(definitionRootFolder, "pom.xml", freemarkers.templatize("pomdefinition", new HashMap<String, Object>() {

            {
                put("generatedGroupId", groupId);
                put("generatedParentArtifactId", rootFolder.getName());
                put("generatedArtifactId", definitionRootFolder.getName());
                put("generatedVersion", version);
                put("generatedName", family.getName() + " :: Definition");
                put("rootPackage", rootPackage);
            }
        }));
        doWrite(runtimeRootFolder, "pom.xml", freemarkers.templatize("pomruntime", new HashMap<String, Object>() {

            {
                put("generatedGroupId", groupId);
                put("generatedParentArtifactId", rootFolder.getName());
                put("generatedArtifactId", runtimeRootFolder.getName());
                put("generatedDefinitionArtifactId", definitionRootFolder.getName());
                put("generatedVersion", version);
                put("generatedName", family.getName() + " :: Runtime");
                put("componentArtifactId", artifactId);
            }
        }));
        doWrite(rootFolder, "pom.xml", freemarkers.templatize("pomparent", new HashMap<String, Object>() {

            {
                put("generatedGroupId", groupId);
                put("generatedArtifactId", rootFolder.getName());
                put("generatedRuntimeArtifactId", runtimeRootFolder.getName());
                put("generatedDefinitionArtifactId", definitionRootFolder.getName());
                put("generatedVersion", version);
                put("generatedName", family.getName());
                put("componentsApiVersion", componentsApiVersion);
                put("componentNgApiVersion", componentNgApiVersion);
                try {
                    put("local_m2", fakeM2.toURI().toURL());
                } catch (final MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }));
    }

    private void doAttach(final Collection<File> modules) {
        if (attach) {
            modules.forEach(module -> {
                getLog().info("Preparing component artifacts for " + module.getName());
                if (mvnPackage(module) != 0) {
                    throw new IllegalStateException("Compilation of " + module + " failed, please check the output");
                }

                attachModule(module, "definition");
                attachModule(module, "runtime");
                if (studioHome != null) {
                    installInStudio(module);
                }
            });
        } else if (studioHome != null) {
            getLog().info("Ignoring studio deployment since attach=false");
        }
    }

    private void installInStudio(final File module) {
        getLog().info("Installing development version of " + module.getName() + " in " + studioHome);

        final List<String> artifacts = asList(artifactId + '-' + version + ".jar",
                module.getName() + "-runtime-" + version + ".jar", module.getName() + "-definition-" + version + ".jar");

        // 0. remove staled libs from the cache (configuration/org.eclipse.osgi)
        final File osgiCache = new File(studioHome, "configuration/org.eclipse.osgi");
        if (osgiCache.isDirectory()) {
            ofNullable(osgiCache.listFiles(child -> {
                try {
                    return child.isDirectory() && Integer.parseInt(child.getName()) > 0;
                } catch (final NumberFormatException nfe) {
                    return false;
                }
            })).map(Stream::of).orElseGet(Stream::empty).map(id -> new File(id, ".cp")).filter(File::exists).flatMap(
                    cp -> ofNullable(cp.listFiles((dir, name) -> name.endsWith(".jar"))).map(Stream::of).orElseGet(Stream::empty))
                    .filter(jar -> artifacts.contains(jar.getName())).forEach(this::tryDelete);
        }

        // 1. remove staled libs from the build (workspace/.Java/lib/)
        final File javaLib = new File(studioHome, "workspace/.Java/lib");
        if (javaLib.isDirectory()) {
            ofNullable(javaLib.listFiles((d, n) -> artifacts.contains(n))).map(Stream::of).orElseGet(Stream::empty)
                    .forEach(this::tryDelete);
        }

        // 2. install the definition in plugin
        final File definition = new File(module, module.getName() + "-definition/target")
                .listFiles((d, n) -> n.endsWith(".jar") && !n.endsWith("-sources.jar"))[0];
        if (!definition.exists()) {
            throw new IllegalArgumentException("No definition found at " + definition);
        }
        try {
            final File target = new File(studioHome, "plugins/" + definition.getName());
            Files.copy(definition.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLog().info("Installed " + definition.getName() + " at " + target.getAbsolutePath());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        // 3. install the runtime dependency tree (scope compile+runtime) in the studio m2 repo
        final File configIni = new File(studioHome, "configuration/config.ini");
        final Properties config = new Properties();
        if (configIni.exists()) {
            try (final InputStream is = new FileInputStream(configIni)) {
                config.load(is);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        final String repoType = config.getProperty("maven.repository");
        if (!"global".equals(repoType)) {
            Stream.of(artifactId, module.getName() + "-definition", module.getName() + "-runtime").forEach(art -> {
                try {
                    final File target = new File(studioHome, "configuration/.m2/repository/" + groupId.replace('.', '/') + '/'
                            + art + '/' + version + '/' + art + '-' + version + ".jar");
                    mkdirP(target.getParentFile());
                    Files.copy(definition.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    getLog().info("Installed " + art + " at " + target.getAbsolutePath());
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } else {
            getLog().info("Studio " + studioHome + " configured to use global maven repository, skipping artifact installation");
        }
    }

    private void tryDelete(File jar) {
        if (!jar.delete()) {
            getLog().warn("Can't delete " + jar.getAbsolutePath());
        } else {
            getLog().info("Deleting " + jar.getAbsolutePath());
        }
    }

    private ServiceMeta.ActionMeta hasGuessSchemaAction(final ContainerComponentRegistry registry,
            final ComponentFamilyMeta.BaseMeta<?> meta) {
        return registry.getServices().stream().flatMap(s -> s.getActions().stream())
                .filter(a -> a.getType().equals("schema") && a.getFamily().equals(meta.getParent().getName()) && Stream
                        .concat(meta.getParameterMetas().stream(),
                                meta.getParameterMetas().stream().flatMap(p -> p.getNestedParameters().stream()))
                        .anyMatch(p -> a.getAction().equals(p.getMetadata().get(ActionParameterEnricher.META_PREFIX + "schema"))))
                .findFirst().orElse(null);
    }

    // this needs some revisit to be more robust
    private String generateGuessSchema(final ComponentFamilyMeta family, final ComponentFamilyMeta.BaseMeta<?> mapper,
            final Map<String, ParameterValue> parametersMap, final String runtimeArtifactId,
            final ServiceMeta.ActionMeta guessSchema, final String datasetPrefix, final Class<?> inputType,
            final Class<?> outputType, final String componentClass) {
        final StringBuilder out = new StringBuilder();
        out.append("    private <T> T executeInSandbox(final String clazz, final UnsafeFunction<Object, T> supplier) {\n"
                + "        final RuntimeInfo runtimeInfo;\n" + "        try {\n"
                + "            runtimeInfo = new JarRuntimeInfo(new URL(\"mvn:")

                .append(groupId).append("/").append(runtimeArtifactId).append("/").append(version).append("\"),")
                .append("\n" + "                    DependenciesReader.computeDependenciesFilePath(\"").append(groupId)
                .append("\", \"").append(runtimeArtifactId).append("\"),\n                    clazz) {\n")
                // enforce slf4j(+simple) usage to be able to log in the sandbox classloader whatever env we are in
                .append("                @Override\n" + "                public List<URL> getMavenUrlDependencies() {\n")
                .append("                    final List<URL> $$urls = super.getMavenUrlDependencies();\n")
                .append("                    final Optional<URL> $$slf4jApi = $$urls.stream()\n")
                .append("                                                   .filter(u -> u.getFile().contains(\"/slf4j-api/\"))\n")
                .append("                                                   .findFirst();\n")
                .append("                    if (!$$slf4jApi.isPresent()) {\n").append("                        try {\n")
                .append("                            $$urls.add(new URL(\"mvn:org.talend.libraries/slf4j-api-1.7.10/6.1.0\"));\n")
                .append("                        } catch (final MalformedURLException e) {\n")
                .append("                            throw new IllegalArgumentException(e);\n" + "                        }\n")
                .append("                    }\n")
                .append("                    final Optional<URL> $$slf4jImpl = $$urls.stream()\n")
                .append("                                                           .filter(u -> u.getFile().contains(\"/slf4j-\") && !u.getFile().contains(\"/slf4j-api/\"))\n")
                .append("                                                           .findFirst();\n")
                // todo: also check if there a slf4j binding in the classpath
                .append("                    if (!$$slf4jImpl.isPresent()) {\n").append("                        try {\n")
                .append("                            $$urls.add(new URL(\"mvn:org.talend.libraries/slf4j-simple-1.7.2/6.0.0\"));\n")
                .append("                        } catch (final MalformedURLException e) {\n")
                .append("                            throw new IllegalArgumentException(e);\n")
                .append("                        }\n").append("                    }\n")
                .append("                    return $$urls;\n").append("                }\n")
                .append("            };\n        } catch (final MalformedURLException e) {\n"
                        + "            throw new IllegalArgumentException(e);\n" + "        }\n"
                        + "        try (final SandboxedInstance instance = RuntimeUtil.createRuntimeClass(runtimeInfo, getClass().getClassLoader())) {\n"
                        + "            return supplier.apply(instance.getInstance());\n"
                        + "        } catch (final java.lang.reflect.InvocationTargetException ite) {\n"
                        + "            throw new IllegalStateException(ite.getCause());\n"
                        + "        } catch (final Exception e) {\n" + "            throw new IllegalStateException(e);\n"
                        + "        }\n" + "    }\n\n");
        if (inputType != null) {
            out.append("    @Override\n"
                    + "    public Schema getSchema(final Connector connector, final boolean isOutgoingConnection) {\n"
                    + "        if (isOutgoingConnection) {\n" + "            if (\"MAIN\".equals(connector.getName())) {\n"
                    + "                if (!\"EmptyRecord\".equals($$internalSchema.schema.getValue().getName())) {\n"
                    + "                    return $$internalSchema.schema.getValue();\n" + "                }\n"
                    + "            }\n" + "            if (\"REJECT\".equals(connector.getName())) {\n"
                    + "                if (!\"EmptyRecord\".equals($$internalRejectSchema.schema.getValue().getName())) {\n"
                    + "                    return $$internalRejectSchema.schema.getValue();\n" + "                }\n"
                    + "            }\n"
                    + "        } else if (!\"EmptyRecord\".equals($$internalFlowSchema.schema.getValue().getName())) {\n"
                    + "            return $$internalFlowSchema.schema.getValue();\n" + "        }\n" + "        return ")
                    .append("executeInSandbox(\"").append(componentClass).append("\", ").append("o -> {\n")
                    .append("            final ClassLoader $$correctLoader = ClassLoader.class.cast(o.getClass().getMethod(\"componentClassLoader\").invoke(null));\n")
                    .append("            final String $$recordTypeName = !isOutgoingConnection ? \"").append(inputType.getName())
                    .append("\" : \"").append((outputType == null ? inputType : outputType).getName()).append("\";\n")
                    .append("            final Class<?> $$recordType = $$correctLoader.loadClass(").append("$$recordTypeName);\n")
                    .append("            final Object $$converter = $$correctLoader.loadClass(")
                    .append("\"org.talend.sdk.component.runtime.avro.ComponentModelToIndexeredRecordConverter\")\n")
                    .append("                                         .getConstructor().newInstance();\n")
                    .append("            final Object $$meta = $$converter.getClass().getMethod(\"getMeta\", Class.class).invoke($$converter, $$recordType);\n")
                    .append("            return Schema.class.cast($$meta.getClass().getMethod(\"getSchema\").invoke($$meta));\n")
                    .append("        });\n    }\n\n");
        } else {
            out.append("    @Override\n" + "    public Schema getSchema(final Connector connector, final boolean "
                    + "isOutgoingConnection) {\n"
                    + "        return isOutgoingConnection && !\"EmptyRecord\".equals($$internalSchema.schema.getValue().getName()) ? "
                    + "$$internalSchema.schema.getValue() : (isOutgoingConnection ? ").append("discoverSchema()")
                    .append(" : null);\n    }\n\n");

            // this is not the most sexy part of the code but the simplest way to make the studio understanding it
            out.append("    private Schema discoverSchema() {\n");
            if (guessSchema != null) { // delegate to the action
                out.append("        validate$$internalGuessSchema();\n");
                out.append("        return $$internalSchema.schema.getValue();\n");
            } else { // try to read a record and deduce the schema
                out.append("        final Map<String, String> $$configuration = new HashMap<>();\n");
                final String filteredPrefix = datasetPrefix == null ? "" : datasetPrefix;
                parametersMap.entrySet().stream().filter(e -> e.getKey().startsWith(filteredPrefix)).forEach(e -> {
                    final String key = e.getKey();

                    out.append("        if (");
                    e.getValue().conditions.forEach(c -> out.append(c.substring(ROOT_PROPERTIES.length())).append(" && "));
                    out.append("true) {\n");
                    final String value = e.getValue().getValue(); // note it can include some casting
                    if (key.endsWith("[]")) {
                        final String prefix = key.substring(0, key.length() - "[]".length());
                        final String listVar = value.substring(ROOT_PROPERTIES.length());
                        out.append("            IntStream.range(0, ").append(listVar)
                                .append(".size()).forEach(index -> $$configuration.put" + "(\"").append(prefix)
                                .append("[\" + index + \"]\", ").append(listVar).append(".get(index)));\n");
                    } else {
                        final int rpIdx = value.indexOf(ROOT_PROPERTIES);
                        out.append("            $$configuration.put(\"").append(key).append("\", ")
                                .append(value.substring(0, rpIdx)).append(value.substring(rpIdx + ROOT_PROPERTIES.length()))
                                .append(");\n");
                    }
                    out.append("        }\n");
                });
                out.append("        return executeInSandbox(\"org.talend.sdk.component.runtime.avro.SchemaDiscoverer\", "
                        + "instance -> {\n" + "            final Class<?> clazz = instance.getClass();\n" + "      "
                        + "      final Method find = clazz.getMethod(\"find\", String.class, String.class, String.class, int"
                        + ".class, Map.class);\n" + "            return Schema.class.cast(find.invoke(instance, \"")
                        .append(groupId).append(':').append(artifactId).append(':').append(version).append("\", \"")
                        .append(family.getName()).append("\", \"").append(mapper.getName()).append("\", 1, $$configuration));\n")
                        .append("        });\n");
            }
            out.append("    }\n\n");
        }

        if (guessSchema != null) {
            out.append("    public ValidationResult validate$$internalGuessSchema() {\n")
                    .append("        final Map<String, String> $$configuration = new HashMap<>();\n");

            // for @DiscoverSchema action the only supported parameter is the dataset, if not used it is not that important
            // since our factory will ignore it so easier to just set it up anyway
            final String actionPrefix = guessSchema.getParameters().stream().map(ParameterMeta::getPath).findFirst()
                    .filter(p -> !p.isEmpty()).map(p -> p + '.').orElse("");
            final String datasetPrefixWithSeparator = datasetPrefix == null ? "" : (datasetPrefix + '.');
            parametersMap.forEach((k, v) -> {
                out.append("        if (");
                v.conditions.forEach(c -> out.append(c.substring(ROOT_PROPERTIES.length())).append(" && "));
                out.append(" true) {\n");
                final String value = v.getValue();
                final int rootPropIdx = value.indexOf(ROOT_PROPERTIES);
                out.append("            $$configuration.put(\"").append(actionPrefix)
                        .append(k.substring(datasetPrefixWithSeparator.length())).append("\", ")
                        .append(value.substring(0, rootPropIdx)).append(value.substring(rootPropIdx + ROOT_PROPERTIES.length()))
                        .append(");\n");
                out.append("        }\n");
            });

            out.append("        try {\n");
            out.append("            return executeInSandbox(\"org.talend.sdk.component.runtime.avro.SchemaDiscoverer\", "
                    + "instance -> {\n                final Class<?> clazz = instance.getClass();\n" + "      " + ""
                    + "          final Method populateSchema = clazz.getMethod(\"populateSchema\", String"
                    + ".class, String.class, String" + ".class, String.class, String.class, Map.class);\n"
                    + "                final Schema schema = Schema.class" + ".cast(populateSchema.invoke(instance, \"")
                    .append(groupId).append(':').append(artifactId).append(':').append(version).append("\", \"")
                    .append(family.getName()).append("\", \"").append(guessSchema.getAction()).append("\", \"")
                    .append(guessSchema.getType()).append("\", \"").append(family.getName()).append("_").append(mapper.getName())
                    .append("_record")
                    .append("\", $$configuration));\n                $$internalSchema.schema.setValue(schema);\n"
                            + "                return ValidationResult.OK;\n")
                    .append("            });\n" + "        } catch (final RuntimeException re) {\n"
                            + "            if (java.lang.reflect.InvocationTargetException.class.isInstance(re)) {\n"
                            + "                return new ValidationResult(ValidationResult.Result.WARNING, re.getCause().getMessage());\n"
                            + "            }\n"
                            + "            return new ValidationResult(ValidationResult.Result.WARNING, re.getMessage());\n"
                            + "        }\n");
            out.append("    }\n\n");
        } else if (inputType == null) { // do one based on the first record
            out.append("    public ValidationResult validate$$internalGuessSchema() {\n")
                    .append("        final Schema $$schema = discoverSchema();\n")
                    .append("        $$internalSchema.schema.setValue($$schema);\n")
                    .append("        return ValidationResult.OK;\n").append("    }\n\n");
        }
        out.append(
                "    public void after$$internalGuessSchema() {\n        // no-op: forces the studio to refresh the schema\n    }\n\n");

        out.append(
                "    private interface UnsafeFunction<PARAMETER, RETURN> {\n        RETURN apply(PARAMETER parameter) throws Exception;\n    }\n\n");
        return out.toString();
    }

    private void attachModule(final File module, final String type) {
        final File child = new File(module, module.getName() + "-" + type);
        final ArtifactHandler handler = artifactHandlerManager.getArtifactHandler("jar");

        {
            final File[] binary = new File(child, "target").listFiles((dir, name) -> name.startsWith(child.getName() + '-')
                    && name.endsWith(".jar") && !name.endsWith("-sources.jar"));
            assertOne(child, binary);

            getLog().info("Attaching " + binary[0].getName());

            final Artifact artifact = new DefaultArtifact(groupId, child.getName(), version, project.getArtifact().getScope(),
                    "jar", null, handler);
            artifact.setFile(binary[0]);
            artifact.setResolved(true);
            project.addAttachedArtifact(artifact);
        }

        {
            final File[] sources = new File(child, "target")
                    .listFiles((dir, name) -> name.startsWith(child.getName() + '-') && name.endsWith("-sources.jar"));
            assertOne(child, sources);
            getLog().info("Attaching " + sources[0].getName());
            final Artifact artifact = new DefaultArtifact(groupId, child.getName(), version, project.getArtifact().getScope(),
                    "jar", "sources", handler);
            artifact.setFile(sources[0]);
            artifact.setResolved(true);
            project.addAttachedArtifact(artifact);
        }
    }

    private Map<String, ParameterValue> buildParametersMap(final String prefix, final Collection<ParameterMeta> parameterMetas,
            final Collection<String> conditions) {
        if (parameterMetas == null || parameterMetas.isEmpty()) {
            return emptyMap();
        }

        final Map<String, ParameterValue> out = new HashMap<>();
        parameterMetas.forEach(p -> {
            final String currentParam = prefix + toParamName(p);
            final String currentValue = currentParam + ".getValue()";

            final Collection<String> paramConditions = new ArrayList<>(conditions);
            paramConditions.add(currentParam + " != null");

            switch (p.getType()) {
            case ARRAY:
                paramConditions.add(currentValue + " != null");
                out.put(p.getPath() /* todo: when list support will actually be added + "[]" */,
                        new ParameterValue(paramConditions, currentValue));
                break;
            case OBJECT:
                out.putAll(buildParametersMap(currentParam + '.', p.getNestedParameters(), paramConditions));
                break;
            case BOOLEAN:
                paramConditions.add(currentValue + " != null");
                out.put(p.getPath(), new ParameterValue(paramConditions, "Boolean.toString(" + currentValue + ")"));
                break;
            case STRING:
                paramConditions.add(currentValue + " != null");
                paramConditions.add(currentValue + ".length() != 0");
                out.put(p.getPath(), new ParameterValue(paramConditions, currentValue));
                break;
            case ENUM:
                paramConditions.add(currentValue + " != null");
                paramConditions.add(currentValue + ".length() != 0");
                out.put(p.getPath(), new ParameterValue(paramConditions, currentValue /* + ".name()" */));
                break;
            case NUMBER:
                paramConditions.add(currentValue + " != null");
                out.put(p.getPath(), new ParameterValue(paramConditions, "Number.class.cast(" + currentValue + ").toString()"));
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + p.getType());
            }
        });
        return out;
    }

    private void assertOne(final File identifierFile, final File[] found) {
        if (found == null || found.length == 0) {
            throw new IllegalStateException("No module was built for " + identifierFile);
        }
        if (found.length > 1) {
            throw new IllegalStateException("Ambiguous module for " + identifierFile + ", found " + asList(found));
        }
    }

    private int mvnPackage(final File root) {
        final String mavenHome = Stream
                .of(System.getProperty("maven.home"), System.getenv().get("M2_HOME"), System.getenv().get("MAVEN_HOME"))
                .filter(Objects::nonNull).findFirst().orElseThrow(
                        () -> new IllegalStateException("No maven environment set, please set one before launching the build"));
        final File mvnScript = new File(mavenHome,
                "bin/" + (System.getProperty("os.name", "unknown").toLowerCase(ENGLISH).contains("win") ? "mvn.cmd" : "mvn"));
        if (!mvnScript.exists()) {
            throw new IllegalStateException(
                    "mvn is not setup on your machine, please set M2_HOME environment variable or maven.home system property");
        }

        try {
            final Process process = new ProcessBuilder(mvnScript.getAbsolutePath(), "clean", "package").inheritIO()
                    .redirectErrorStream(

                            true)
                    .directory(root).start();
            process.waitFor(10, MINUTES);
            return process.exitValue();
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            throw new IllegalStateException(e);
        }
    }

    private boolean hasOutput(final ComponentFamilyMeta.ProcessorMeta processor) {
        return !buildOutputs(findListener(processor)).isEmpty();
    }

    private void generateI18n(final Container container, final File componentRoot, final ComponentFamilyMeta.BaseMeta<?> meta,
            final String packageForJava) {
        final File i18n = new File(componentRoot,
                "src/main/resources/" + packageForJava.replace('.', '/') + "/messages.properties");
        appendProperties(i18n, new Properties() {

            {
                final String displayName = meta.findBundle(container.getLoader(), Locale.ENGLISH).displayName()
                        .orElse(meta.getName());

                setProperty("component." + meta.getName() + ".title", displayName);
                setProperty("component." + meta.getName() + ".displayName", displayName);
            }
        });
    }

    private void appendProperties(final File i18n, final Properties newValues) {
        mkdirP(i18n.getParentFile());

        final Properties content = new Properties();
        if (i18n.exists()) {
            try (final InputStream is = new FileInputStream(i18n)) {
                content.load(is);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        content.putAll(newValues);
        try (final OutputStream os = new FileOutputStream(i18n)) {
            content.store(os, "Generated by Talend " + getClass().getSimpleName());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String findCommon(final Collection<String> packages) {
        if (packages.size() == 1) {
            return packages.iterator().next();
        }
        String current = packages.iterator().next();
        while (current != null) {
            final String currentRef = current;
            if (packages.stream().allMatch(p -> p.startsWith(currentRef))) {
                return current;
            }
            final int parentIdx = current.lastIndexOf('.');
            if (parentIdx <= 0) {
                break;
            }
            current = current.substring(0, parentIdx);
        }
        // fallback on the first one (random but functional)
        return packages.iterator().next();
    }

    // easier than using a template since 1 entry point implies multiple files, todo: simplify this code with eugene engine?
    private void generateProperties(final ClassLoader loader, final File folder, final String className,
            final Definition definition, final List<ParameterMeta> parameterMetas, final boolean hasOutput,
            final ServiceMeta.ActionMeta guessSchema, final Supplier<String> schemaAccessor, final boolean isProcessor,
            final ParameterMeta rootParam) {
        final StringBuilder output = new StringBuilder("package " + definition.packageName + ";\n\n"
                + "import java.util.Optional;\nimport java.util.stream.Stream;\nimport java.lang.reflect.Method;\n"
                + "import java.util.HashMap;\nimport java.util.Map;\nimport java.util.List;\n"
                + "import java.net.MalformedURLException;\nimport java.util.stream.IntStream;\nimport java.util.Arrays;\n"
                + "import java.util.Collections;\nimport java.util.HashSet;\nimport java.util.Set;\nimport java.util.EnumSet;\n\n"
                + "import org.apache.avro.Schema;\n" + "import org.apache.avro.SchemaBuilder;\n"
                + "import org.talend.daikon.avro.SchemaConstants;\n" + "import org.talend.daikon.avro.SchemaConstants;\n"
                + "import org.talend.components.api.properties.ComponentPropertiesImpl;\n"
                + "import org.talend.daikon.runtime.RuntimeInfo;\n" + "import java.net.URL;\n"
                + "import org.talend.daikon.sandbox.SandboxedInstance;\n"
                + "import org.talend.components.api.component.runtime.JarRuntimeInfo;\n"
                + "import org.talend.components.api.component.runtime.DependenciesReader;\n"
                + "import org.talend.components.api.component.Connector;\n"
                + "import org.talend.components.api.component.PropertyPathConnector;\n"
                + "import org.talend.components.common.SchemaProperties;\n" + "import org.talend.daikon.runtime.RuntimeUtil;\n"
                + "import org.talend.daikon.properties.PresentationItem;\n"
                + "import org.talend.daikon.properties.presentation.Form;\n"
                + "import org.talend.daikon.properties.presentation.Widget;\n"
                + "import org.talend.daikon.properties.ValidationResult;\n"
                + "import org.talend.daikon.properties.property.Property;\n"
                + "import org.talend.daikon.properties.property.PropertyFactory;\n\n" + "public class " + className
                + " extends ComponentPropertiesImpl {\n");

        final String order = rootParam == null ? null : rootParam.getMetadata().get("tcomp::ui::optionsorder::value");
        if (order != null) {
            final List<String> propertiesOrder = new ArrayList<>(asList(order.split(",")));
            parameterMetas.sort((o1, o2) -> {
                final int i = propertiesOrder.indexOf(o1.getName()) - propertiesOrder.indexOf(o2.getName());
                return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
            });
        }

        parameterMetas.forEach(p -> {
            final String name = toParamName(p);
            final Type javaType = p.getJavaType();
            final boolean required = "true".equalsIgnoreCase(p.getMetadata().get("tcomp::validation::required"));
            switch (p.getType()) {
            case ARRAY: {
                final Class<?> generic = Class.class.cast(ParameterizedType.class.cast(javaType).getActualTypeArguments()[0]);
                if (generic != String.class) {
                    throw new IllegalArgumentException("For now only List<String> are supported in configuration/properties");
                }
                // for now only support List<String>
                output.append("    public Property<String> ").append(name).append(" = PropertyFactory.newString(\"").append(name)
                        .append("\")");
                if ("true".equalsIgnoreCase(p.getMetadata().get("tcomp::ui::credential"))) {
                    output.append(".setFlags(EnumSet.of(Property.Flags.ENCRYPT, Property.Flags.SUPPRESS_LOGGING))");
                }
                appendRequired(output, required);
                output.append(";\n");
                break;
            }
            case OBJECT: {
                final String clazz = Class.class.cast(javaType).getSimpleName().replace("$", "");
                final String propertyClazz = className + clazz + "Properties";
                output.append("    public ").append(propertyClazz).append(" ").append(name).append(" = new ")
                        .append(propertyClazz).append("(\"").append(name).append("\");\n");
                final List<ParameterMeta> metas = ofNullable(p.getNestedParameters()).orElse(emptyList());
                generateProperties(loader, folder, propertyClazz, definition, metas, false, null, null, false, p);
                break;
            }
            case BOOLEAN:
                output.append("    public Property<Boolean> ").append(name).append(" = PropertyFactory.newBoolean(\"")
                        .append(name).append("\", false)");
                appendRequired(output, required);
                if (rootParam != null) {
                    appendDefaultIfNeeded(output, rootParam.getJavaType(), p, Object::toString);
                }
                output.append(";\n");
                break;
            case STRING:
                output.append("    public Property<String> ").append(name).append(" = PropertyFactory.newString(\"").append(name)
                        .append("\")");
                if ("true".equalsIgnoreCase(p.getMetadata().get("tcomp::ui::credential"))) {
                    output.append(".setFlags(EnumSet.of(Property.Flags.ENCRYPT, Property.Flags.SUPPRESS_LOGGING))");
                }
                appendRequired(output, required);
                if (rootParam != null) {
                    appendDefaultIfNeeded(output, rootParam.getJavaType(), p,
                            v -> "\"" + v.toString().replace("\"", "\\\"") + "\"");
                }
                output.append(";\n");
                break;
            case ENUM: { // seen as string since we don't have the enum loaded in this module
                output.append("    public Property<String> ").append(name).append(" = PropertyFactory.newString(\"").append(name)
                        .append("\")");
                appendRequired(output, required);
                if (rootParam != null) {
                    appendDefaultIfNeeded(output, rootParam.getJavaType(), p, v -> "\"" + v + "\"");
                }
                output.append(".setPossibleValues(").append(p.getProposals().stream().collect(joining("\", \"", "\"", "\"")))
                        .append(")");
                output.append(";\n");
                break;
            }
            case NUMBER:
                if (p.getJavaType() == int.class || p.getJavaType() == Integer.class) {
                    output.append("    public Property<Integer> ").append(name).append(" = PropertyFactory.newInteger(\"")
                            .append(name).append("\")");
                } else if (p.getJavaType() == double.class || p.getJavaType() == Double.class) {
                    output.append("    public Property<Double> ").append(name).append(" = PropertyFactory.newDouble(\"")
                            .append(name).append("\")");
                } else if (p.getJavaType() == float.class || p.getJavaType() == Float.class) {
                    output.append("    public Property<Float> ").append(name).append(" = PropertyFactory.newFloat(\"")
                            .append(name).append("\")");
                } else {
                    throw new IllegalArgumentException("Unsupported number: " + p.getJavaType() + " from parameter " + p.getPath()
                            + ", use int/double/float please");
                }
                appendRequired(output, required);
                if (rootParam != null) {
                    appendDefaultIfNeeded(output, rootParam.getJavaType(), p, Object::toString);
                }
                output.append(";\n");
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + p.getType());
            }
        });

        if (isProcessor) {
            output.append(
                    "    public Property<Integer> $$internalChunkSize = PropertyFactory.newInteger(\"$$internalChunkSize\").setValue(10);\n\n");

            output.append(
                    "    public SchemaProperties $$internalRejectSchema = new SchemaProperties(\"$$internalRejectSchema\");\n\n");
            output.append(
                    "    public SchemaProperties $$internalFlowSchema = new SchemaProperties(\"$$internalFlowSchema\");\n\n");
            output.append("    public SchemaProperties $$internalSchema = new SchemaProperties(\"$$internalSchema\") {\n"
                    + "        public void afterSchema() { // propagate the schema\n"
                    + "            if (\"EmptyRecord\".equals($$internalFlowSchema.schema.getValue().getName())) {\n"
                    + "                $$internalFlowSchema.schema.setValue($$internalSchema.schema.getValue());\n"
                    + "            }\n"
                    + "            $$internalRejectSchema.schema.setValue($$internalFlowSchema.schema.getValue());\n        }\n"
                    + "    };\n\n");
        } else if (hasOutput) { // studio needs a schema so give it one to let have an output, to revisit with studio integration
            output.append("    public SchemaProperties $$internalSchema = new SchemaProperties(\"$$internalSchema\");\n\n");
        }
        if (hasOutput && (guessSchema != null || !isProcessor)) {
            output.append(
                    "    public final transient PresentationItem $$internalGuessSchema = new PresentationItem(\"$$internalGuessSchema\");\n\n");
        }

        output.append("\n");
        output.append("    public ").append(className).append("(final String name) {").append("\n");
        output.append("        super(name);\n");
        output.append("    }\n");
        output.append("\n");

        // here we define the connectors of the component, this code can be simplified but for now it is ok and just a bridge
        // anyway
        if (hasOutput) {
            output.append("    @Override\n    public Set<Connector> getPossibleConnectors(final boolean isOutputConnection) {\n");
            if (isProcessor) {
                output.append("        return isOutputConnection ? ");
                output.append("new HashSet<>(Arrays.asList(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalSchema\"), "
                        + "new PropertyPathConnector(Connector.REJECT_NAME, \"$$internalRejectSchema\")))")
                        .append(" : Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalFlowSchema\"));\n");
            } else {
                output.append("        return isOutputConnection ? ").append(
                        "Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalSchema\")) : Collections.emptySet();\n");

            }
            output.append("    }\n\n");
            if (isProcessor) {
                output.append("    @Override\n    public Set<Connector> getAvailableConnectors("
                        + "final Set<? extends Connector> existingConnectors, final boolean isOutputConnection) {\n")
                        .append("        if (isOutputConnection) {\n")
                        .append("            final Set<Connector> connectors = new HashSet<>();\n")
                        .append("            connectors.add(new PropertyPathConnector(Connector.REJECT_NAME, \"$$internalRejectSchema\"));\n")
                        .append("            connectors.add(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalSchema\"));\n")
                        .append("            if (existingConnectors != null && !existingConnectors.isEmpty()) {\n")
                        .append("                connectors.removeAll(existingConnectors);\n").append("            }\n")
                        .append("            return connectors;\n").append("        } else {\n")
                        .append("            return (existingConnectors == null || existingConnectors.isEmpty()) ?\n")
                        .append("                Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalFlowSchema\")) : \n")
                        .append("                Collections.emptySet();\n").append("        }\n").append("    }\n\n");
            } else {
                output.append("    @Override\n    public Set<Connector> getAvailableConnectors("
                        + "final Set<? extends Connector> existingConnectors, final boolean isOutputConnection) {\n"
                        + "        return isOutputConnection && (existingConnectors == null || existingConnectors.isEmpty()) ? "
                        + "Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalSchema\")) : "
                        + "Collections.emptySet();\n" + "    }\n\n");
            }

            if (schemaAccessor != null) {
                output.append(schemaAccessor.get());
            }
        } else if (isProcessor) {
            output.append("    @Override\n    public Set<Connector> getPossibleConnectors(final boolean isOutputConnection) {\n")
                    .append("        return isOutputConnection ? ")
                    .append("Collections.singleton(new PropertyPathConnector(Connector.REJECT_NAME, \"$$internalRejectSchema\")) : ")
                    .append("Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalFlowSchema\"));\n")
                    .append("    }\n\n");
            output.append("    @Override\n    public Set<Connector> getAvailableConnectors("
                    + "final Set<? extends Connector> existingConnectors, final boolean isOutputConnection) {\n")
                    .append("        if (isOutputConnection) {\n")
                    .append("            return (existingConnectors == null || existingConnectors.isEmpty()) ?\n")
                    .append("                Collections.singleton(new PropertyPathConnector(Connector.REJECT_NAME, \"$$internalRejectSchema\")) : \n")
                    .append("                Collections.emptySet();\n").append("        } else {\n")
                    .append("            return (existingConnectors == null || existingConnectors.isEmpty()) ?\n")
                    .append("                Collections.singleton(new PropertyPathConnector(Connector.MAIN_NAME, \"$$internalSchema\")) : \n")
                    .append("                Collections.emptySet();\n").append("        }\n").append("    }\n\n");
        }

        // handle @ActiveIf - for now we only support links which are at the same level - to be enhanced
        final Collection<String> visibilityMethods = findActiveIfCallbacks(parameterMetas);

        output.append("    @Override\n    public void setupLayout() {\n        super.setupLayout();\n\n")
                .append("        final Form $$main = new Form(this, Form.MAIN);\n");
        if (rootParam != null) {
            output.append("        $$main.setTitle(\"").append(
                    rootParam.findBundle(loader, Locale.ENGLISH).displayName().orElseGet(() -> capitalise(rootParam.getName())))
                    .append("\");\n");
        }
        final Map<String, String> formByProperty = buildForm(parameterMetas, hasOutput, guessSchema, isProcessor, output,
                rootParam == null ? emptyMap() : rootParam.getMetadata());

        // before exiting the setup, ensure to init all visibilities
        visibilityMethods.forEach(m -> output.append("        ").append(m).append("();\n"));

        output.append("    }\n");

        addActiveIfCallbacks(parameterMetas, output, formByProperty);

        output.append("}");
        doWrite(folder, className + ".java", output.toString());

        // i18n
        File i18n = folder;
        { // go back to src/main to add it in resources
            final int packageSegments = definition.getPackageName().split("\\.").length;
            for (int i = 0; i < packageSegments; i++) {
                i18n = i18n.getParentFile();
            }
            i18n = new File(i18n.getParentFile() /* java */,
                    "resources/" + definition.packageName.replace('.', '/') + '/' + className + ".properties");
        }
        appendI18n(loader, parameterMetas, hasOutput, isProcessor, i18n,
                formByProperty.values().stream().distinct().collect(toSet()), rootParam);
    }

    private void appendI18n(final ClassLoader loader, final List<ParameterMeta> parameterMetas, final boolean hasOutput,
            final boolean isProcessor, final File i18n, final Set<String> forms, final ParameterMeta rootParam) {
        appendProperties(i18n, new Properties() {

            {
                if (hasOutput) {
                    setProperty("property.$$internalSchema.displayName", "Schema");
                    setProperty("presItem.$$internalGuessSchema.displayName", "Guess Schema");
                }
                if (isProcessor) {
                    setProperty("property.$$internalFlowSchema.displayName", "Flow Schema");
                    setProperty("property.$$internalRejectSchema.displayName", "Reject Schema");
                    setProperty("property.$$internalChunkSize.displayName", "Chunk Size");
                }
                parameterMetas.forEach(p -> setProperty("property." + p.getName() + ".displayName",
                        p.findBundle(loader, Locale.ENGLISH).displayName().orElse(p.getName())));

                if (rootParam != null) {
                    final String displayName = rootParam.findBundle(loader, ENGLISH).displayName().orElse(rootParam.getName());
                    forms.forEach(f -> {
                        setProperty("form." + f + ".title", displayName);
                        setProperty("form." + f + ".displayName", displayName);
                    });
                } else {
                    forms.forEach(f -> {
                        setProperty("form." + f + ".title", f);
                        setProperty("form." + f + ".displayName", f);
                    });
                }
            }
        });
    }

    // this is not the most sexy way to do it but fine enough for a bridge
    private void appendDefaultIfNeeded(final StringBuilder output, final Type enclosingType, final ParameterMeta param,
            final Function<Object, String> valueMapper) {
        if (!Class.class.isInstance(enclosingType)) {
            return; // not supported for now
        }
        final Class<?> enclosingClass = Class.class.cast(enclosingType);

        // we should read the bytecode and not instantiate it
        try {
            final Object volatileInstance = enclosingClass.getConstructor().newInstance();
            Class<?> current = enclosingClass;
            while (current != Object.class) {
                try {
                    final Field paramField = current.getDeclaredField(param.getName());
                    if (!paramField.isAccessible()) {
                        paramField.setAccessible(true);
                    }
                    final Object def = paramField.get(volatileInstance);
                    if (def != null) {
                        output.append(".setValue(").append(valueMapper.apply(def)).append(")");
                    }
                    break;
                } catch (final NoSuchFieldException | IllegalAccessException e) {
                    current = current.getSuperclass();
                }
            }
        } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(enclosingClass + " is not instantiable", e);
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        }
    }

    private void addActiveIfCallbacks(final List<ParameterMeta> parameterMetas, final StringBuilder output,
            final Map<String, String> formByProperty) {
        filterParametersWithActiveCondition(parameterMetas)
                .collect(groupingBy(p -> p.getMetadata().get("tcomp::condition::if::target"))).forEach((target, value) -> {
                    final String absoluteTarget = resolveTarget(target);
                    if (absoluteTarget.contains(".")) {
                        throw new IllegalArgumentException(
                                "@ActiveIf on undirect field is not yet supported, ensure to use sibling properties");
                    }

                    final String formTarget = getFormName(formByProperty.get(absoluteTarget));
                    final String mtdName = "after" + capitalise(absoluteTarget);
                    output.append("    public void ").append(mtdName).append("() {\n");
                    value.forEach(p -> {
                        final String formParam = getFormName(formByProperty.get(p.getName()));
                        if (formParam == null) {
                            getLog().warn("No form for " + p.getName() + ", skipping");
                            return;
                        }

                        final String[] values = p.getMetadata().getOrDefault("tcomp::condition::if::value", "true").split(",");
                        final String paramName = toParamName(p);
                        output.append("        if(").append(absoluteTarget).append(" != null && ").append(absoluteTarget)
                                .append(".getValue() != null) {\n") //
                                .append("            final Form $$formParam = getForm(").append(formParam).append(");\n")
                                .append("            final Form $$formTarget = ");
                        if (formParam.equals(formTarget)) {
                            output.append("$$formParam");
                        } else {
                            output.append("getForm(").append(formTarget).append(")");
                        }

                        // visible = widget is visible + condition is true
                        output.append(";\n            final boolean $$originalVisible = $$formParam.getWidget(").append(paramName)
                                .append(").isVisible();\n")
                                .append("            final String " + "$$currentValue = String.valueOf(").append(absoluteTarget)
                                .append(".getValue());\n").append("            final boolean $$visible = $$formTarget.getWidget(")
                                .append(absoluteTarget).append(").isVisible() && Stream.of(")
                                .append(Stream.of(values).map(v -> "\"" + v + "\"").collect(joining(",")))
                                .append(").anyMatch(v -> v.equals($$currentValue));\n")
                                .append("            if ($$originalVisible != $$visible) {\n")
                                .append("                $$formParam.getWidget(").append(paramName)
                                .append(").setVisible($$visible);\n");

                        // for now it works cause relationships are only handled with 1 level!
                        final Optional<ParameterMeta> reverseUpdate = parameterMetas.stream().filter(
                                it -> p.getName().equals(resolveTarget(it.getMetadata().get("tcomp::condition::if::target"))))
                                .findFirst();
                        if (reverseUpdate.isPresent()) {
                            output.append("                after").append(capitalise(paramName)).append("();\n");
                        }
                        output.append("            }\n").append("        }\n");
                    });
                    output.append("    }\n\n");
                });
    }

    private Collection<String> findActiveIfCallbacks(final List<ParameterMeta> parameterMetas) {
        return filterParametersWithActiveCondition(parameterMetas)
                .collect(groupingBy(p -> p.getMetadata().get("tcomp::condition::if::target"))).entrySet().stream()
                .map(e -> "after" + capitalise(resolveTarget(e.getKey()))).collect(toList());
    }

    private Stream<ParameterMeta> filterParametersWithActiveCondition(final List<ParameterMeta> parameterMetas) {
        return parameterMetas.stream().filter(p -> p.getMetadata().containsKey("tcomp::condition::if::target"));
    }

    private String resolveTarget(final String target) {
        return target == null ? null : target.replace("./", "");
    }

    private Map<String, String> buildForm(final Collection<ParameterMeta> parameterMetas, final boolean hasOutput,
            final ServiceMeta.ActionMeta guessSchema, final boolean isProcessor, final StringBuilder output,
            final Map<String, String> meta) {
        if (hasOutput) { // added "first" by convention
            output.append("        $$main.addRow($$internalSchema.getForm(Form.REFERENCE));\n");
            if (!isProcessor || guessSchema != null) {
                output.append(
                        "        $$main.addColumn(Widget.widget($$internalGuessSchema).setLongRunning(true).setWidgetType(Widget.BUTTON_WIDGET_TYPE));\n");
            }
        }

        output.append("\n        final Form $$advanced = new Form(this, Form.ADVANCED);\n");
        if (isProcessor) {
            output.append("        $$advanced.addRow($$internalChunkSize);\n");
        }

        final Map<String, String> formByProperty = new HashMap<>();
        final Set<String> forms = meta.keySet().stream()
                .filter(k -> k.startsWith("tcomp::ui::gridlayout::") && k.endsWith("::value")).collect(toSet());
        if (forms.isEmpty()) {

            final Supplier<String> addOperation = "true".equalsIgnoreCase(meta.get("tcomp::ui::horizontallayout"))
                    ? newHorizontalAdderProvider()
                    : () -> "addRow";
            parameterMetas.forEach(p -> addParameter("Form.MAIN", "$$main", output, addOperation, p, formByProperty));
            parameterMetas.stream() // for all params which have an advanced form add it to ensure it is visible into the studio
                    .filter(p -> p.getType() == ParameterMeta.Type.OBJECT).filter(this::hasAdvancedForm)
                    .forEach(p -> addObjectParameter("Form.ADVANCED", "$$advanced", output, toParamName(p)));
        } else {
            // create all forms
            final Map<String, ParameterMeta> paramIndex = parameterMetas.stream()
                    .collect(toMap(ParameterMeta::getName, identity()));
            forms.forEach(key -> {
                final String rawName = getFormName(key);
                final String name = rawName.toLowerCase(ENGLISH).replace("form.", "");

                if (!"Form.MAIN".equals(rawName) && !"Form.ADVANCED".equals(rawName)) { // already declared
                    output.append("\n        final Form $$").append(name).append(" = new Form(this, ").append(rawName)
                            .append(");\n");
                }
                final String layoutDefinition = meta.get(key);
                Stream.of(layoutDefinition.split(Pattern.quote("|"))).forEach(row -> {
                    final Supplier<String> operation = newHorizontalAdderProvider();
                    Stream.of(row.split(",")).forEach(field -> {
                        final ParameterMeta p = paramIndex.get(field);
                        if (p == null) {
                            getLog().warn("No parameter '" + field + "', availables: " + paramIndex.keySet()
                                    + ", check your list in @GridLayout");
                            return;
                        }
                        addParameter(rawName, "$$" + name, output, operation, p, formByProperty);
                    });
                });

                output.append("\n");
            });
        }
        return formByProperty;
    }

    private boolean hasAdvancedForm(final ParameterMeta p) {
        return p.getMetadata().keySet().stream().anyMatch(k -> k.equals("tcomp::ui::gridlayout::Advanced::value"))
                || ofNullable(p.getNestedParameters()).map(Collection::stream).orElseGet(Stream::empty)
                        .anyMatch(this::hasAdvancedForm);
    }

    private String getFormName(final String ref) {
        if (ref == null) {
            return null;
        }

        final String rawName = ref.contains("::")
                ? ref.substring("tcomp::ui::gridlayout::".length(), ref.length() - "::value".length())
                : ref;
        if ("Main".equalsIgnoreCase(rawName)) {
            return "Form.MAIN";
        } else if ("Advanced".equalsIgnoreCase(rawName)) {
            return "Form.ADVANCED";
        } else if ("CitizenUser".equalsIgnoreCase(rawName)) {
            return "Form.CITIZEN_USER";
        } else if (rawName.startsWith("Form.")) {
            return rawName;
        }
        return '"' + rawName + '"';
    }

    private void addParameter(final String formName, final String form, final StringBuilder output,
            final Supplier<String> addOperation, final ParameterMeta p, final Map<String, String> formByProperty) {
        formByProperty.put(p.getName(), formName);
        final String name = toParamName(p);
        switch (p.getType()) {
        case ARRAY: // todo: support other array types like table for objects etc
            if (p.getProposals() != null && !p.getProposals().isEmpty()) {
                output.append("        ").append(form).append(".addColumn(Widget.widget(").append(name)
                        .append(").setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));\n");
                output.append("        ").append(name).append(".setPossibleValues(");
                final String quote = ParameterizedType.class.isInstance(p.getJavaType())
                        && ParameterizedType.class.cast(p.getJavaType()).getActualTypeArguments()[0] == String.class ? "\"" : "";
                p.getProposals().forEach(proposal -> output.append(quote).append(proposal).append(quote).append(","));
                output.setLength(output.length() - 1); // strip last ","
                output.append("\n");
            } else {
                output.append("        ").append(form).append(".addRow(").append(name).append(");\n"); // csv syntax
            }
            break;
        case OBJECT:
            addObjectParameter(formName, form, output, name);
            break;
        case BOOLEAN:
        case STRING:
        case ENUM:
        case NUMBER:
            // todo: handle other widget types
            final String codeType = p.getMetadata().get("tcomp::ui::code::value");
            if (codeType != null && !codeType.isEmpty()) {
                output.append("        ").append(form).append(".").append(addOperation.get()).append("(Widget.widget(")
                        .append(name)
                        .append(").setWidgetType(Widget.CODE_WIDGET_TYPE).setConfigurationValue(Widget.CODE_SYNTAX_WIDGET_CONF, \"")

                        .append(codeType).append("\"));\n");
            } else if (p.getProposals() != null && !p.getProposals().isEmpty()) {
                // likely enums but should be fine for others as well
                output.append("        ").append(form).append(".").append(addOperation.get()).append("(Widget.widget(")
                        .append(name).append(").setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));\n");
            } else {
                output.append("        ").append(form).append(".").append(addOperation.get()).append("(").append(name)
                        .append(");\n");
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported type: " + p.getType());
        }
    }

    private void addObjectParameter(final String formName, final String form, final StringBuilder output, final String name) {
        output.append("        ").append(form).append(".addRow(").append(name).append(".getForm(").append(formName)
                .append("));\n");
    }

    private Supplier<String> newHorizontalAdderProvider() {
        return new Supplier<String>() {

            private boolean first = false;

            @Override
            public String get() {
                if (first) {
                    return "addColumn";
                }
                first = true;
                return "addRow";
            }
        };
    }

    private void appendRequired(final StringBuilder output, final boolean required) {
        if (required) {
            output.append(".setRequired()");
        }
    }

    private String toParamName(final ParameterMeta p) {
        return p.getName().replace('-', '_').replace(' ', '_');
    }

    private void doWrite(final File compFolder, final String name, final String value) {
        mkdirP(compFolder);
        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(compFolder, name)))) {
            stream.write(value.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void mkdirP(final File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Can't create " + dir);
        }
    }

    @Data
    public static class Definition {

        private final String className;

        private final String packageName;

        private final String family;

        private final String name;

        private final Collection<String> categories;

        private final String propertiesClassName;

        private final String runtimeClassName;

        private final Collection<String> topologies;

        private final String icon;

        private final boolean iconExists;
    }

    @Data
    public static class ParameterValue {

        private final Collection<String> conditions;

        private final String value;
    }
}
