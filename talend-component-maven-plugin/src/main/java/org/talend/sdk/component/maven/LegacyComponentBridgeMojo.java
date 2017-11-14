/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.maven;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.apache.maven.shared.utils.StringUtils.capitaliseAllWords;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.maven.legacy.model.CODEGENERATION;
import org.talend.sdk.component.maven.legacy.model.COLUMN;
import org.talend.sdk.component.maven.legacy.model.COMPONENT;
import org.talend.sdk.component.maven.legacy.model.CONNECTOR;
import org.talend.sdk.component.maven.legacy.model.CONNECTORS;
import org.talend.sdk.component.maven.legacy.model.DEFAULT;
import org.talend.sdk.component.maven.legacy.model.DOCUMENTATION;
import org.talend.sdk.component.maven.legacy.model.FAMILIES;
import org.talend.sdk.component.maven.legacy.model.HEADER;
import org.talend.sdk.component.maven.legacy.model.IMPORT;
import org.talend.sdk.component.maven.legacy.model.IMPORTS;
import org.talend.sdk.component.maven.legacy.model.ITEM;
import org.talend.sdk.component.maven.legacy.model.ITEMS;
import org.talend.sdk.component.maven.legacy.model.PARAMETER;
import org.talend.sdk.component.maven.legacy.model.PARAMETERS;
import org.talend.sdk.component.maven.legacy.model.RETURN;
import org.talend.sdk.component.maven.legacy.model.RETURNS;
import org.talend.sdk.component.maven.legacy.model.TABLE;

import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

// todo: move it to an utility able to work from a .jar?
@Deprecated
@Mojo(name = "legacy", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class LegacyComponentBridgeMojo extends ComponentManagerBasedMojo {

    // todo: revisit versions once https://jira.talendforge.org/browse/TUP-18373
    private static final Collection<IMPORT> MANDATORY_DEPENDENCIES = // mandatory runtime dependencies, = tcomp stack
            Stream.concat(Stream.concat(Stream.of("component-api", "component-spi", "component-runtime-impl",
                    "component-runtime-manager", "component-runtime-di").map(artifactId -> {
                        // should be LATEST since we will be bck compat and must use a single version in the classpath
                        // however maven embedder (eclipse) used in the studio doesn't support it and breaks that usage
                        // -> we need to rely on pax mvn everywhere, even in
                        // org.talend.designer.maven.utils.PomUtil#getArtifactPath
                        return newImport(artifactId, "mvn:org.talend.sdk.component/" + artifactId + "/1.0.0-SNAPSHOT", null);
                    }), Stream.of("xbean-finder-shaded", "xbean-asm5-shaded", "xbean-reflect").map(artifactId -> {
                        final String xbeanVersion = AnnotationFinder.class.getPackage().getImplementationVersion();
                        return newImport(artifactId, "mvn:org.apache.xbean/" + artifactId + "/" + xbeanVersion, null);
                    })), Stream.of(newImport("container-core", "mvn:org.talend.sdk.component/container-core/1.0.0-SNAPSHOT", null),
                            // we replace org.apache.geronimo.specs:geronimo-annotation_1.2_spec by the studio one
                            newImport("javax.annotation-api-1.2.jar", "mvn:org.talend.libraries/javax.annotation-api-1.2/6.1.0",
                                    null),
                            // for logging ensure we have slf4j, studio versions
                            newImport("slf4j-api-1.7.10.jar", "mvn:org.talend.libraries/slf4j-api-1.7.10/6.1.0",
                                    "platform:/plugin/org.talend.libraries.slf4j/lib/slf4j-api-1.7.10.jar"),
                            newImport("slf4j-log4j12-1.7.10.jar", "mvn:org.talend.libraries/slf4j-log4j12-1.7.10/6.1.0",
                                    "platform:/plugin/org.talend.libraries.slf4j/lib/slf4j-log4j12-1.7.10.jar")))
                    .collect(toList());

    @Parameter(defaultValue = "true", property = "talend.legacy.attach")
    private boolean attach;

    @Parameter(defaultValue = "legacy", property = "talend.legacy.classifier")
    private String classifier;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File target;

    @Parameter(defaultValue = "${project.groupId}", readonly = true)
    private String groupId;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String version;

    @Parameter(defaultValue = "Talend", property = "talend.legacy.author")
    private String author;

    @Parameter(defaultValue = "true", property = "talend.legacy.defineDependencies")
    private boolean defineDependencies;

    @Parameter(defaultValue = "https://raw.githubusercontent.com/Talend/ui/master/packages/icons/src/svg/", property = "talend.legacy.svgRrepository")
    private String svgRepository;

    @Parameter(defaultValue = "false", property = "talend.legacy.downloadIconsFromGithub")
    private boolean downloadIconsFromGithub;

    @Component
    private MavenProjectHelper mavenProjectHelper;

    private final Freemarkers freemarkers = new Freemarkers(getClass().getSimpleName());

    @Override
    protected void doWork(final ComponentManager manager, final Container container, final ContainerComponentRegistry registry)
            throws MojoExecutionException, MojoFailureException {
        if (!classes.exists()) {
            throw new MojoExecutionException("No " + classes + " so no component available");
        }

        final File zip = new File(target, artifactId + "-" + version + "-" + classifier + ".zip");
        if (zip.exists() && !zip.delete()) {
            throw new MojoExecutionException("Can't delete " + zip);
        }

        final File workDir = new File(zip.getParentFile(), zip.getName().replaceAll(".zip$", "_workdir"));
        mkdirP(workDir);

        // for each component cound, create its folder
        // then generate its model (.xml)
        // its resource bundle (.properties)
        // and its templates (.javajet)

        final JAXBContext modelContext;
        try {
            modelContext = JAXBContext.newInstance(COMPONENT.class);
        } catch (final JAXBException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        registry.getComponents().values().forEach(family -> {
            family.getPartitionMappers().values().forEach(mapper -> {
                final String name = toName(mapper);
                final File componentRoot = new File(workDir, name);
                mkdirP(componentRoot);

                // create the metamodel of the component
                final Collection<String> configKeys = new HashSet<>();
                final Properties internationalization = new Properties();
                internationalization.setProperty("NB_LINE.NAME", "Number of Line");
                internationalization.setProperty("SCHEMA.NAME", "Schema");
                doWrite(componentRoot, name + "_java.xml", stream -> {
                    try {
                        final COMPONENT component = toModel(name, mapper, emptyList(), singletonList("MAIN"),
                                internationalization, container, configKeys);
                        toXml(modelContext, stream, component);
                    } catch (final JAXBException e) {
                        throw new IllegalStateException(e);
                    }
                });

                // store i18n
                doWrite(componentRoot, name + "_messages.properties", stream -> {
                    try {
                        internationalization.store(stream, name + " internationalization");
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                });

                final byte[] iconStream = Icons.findIcon(mapper.getIcon(), downloadIconsFromGithub, svgRepository);
                if (iconStream != null) {
                    doWrite(componentRoot, name + "_icon32.png", stream -> {
                        try {
                            stream.write(iconStream);
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                }

                // create the javajets of the components
                Stream.of("begin", "end", "finally").forEach(part -> doWrite(componentRoot, name + "_" + part + ".javajet",
                        freemarkers.templatize("mapper_" + part, createContext("mapper", mapper, configKeys))));
            });
            family.getProcessors().values().forEach(processor -> { // TODO
                final String name = toName(processor);
                final File componentRoot = new File(workDir, name);
                mkdirP(componentRoot);

                final Collection<String> configKeys = new HashSet<>();
                final Properties internationalization = new Properties();
                internationalization.setProperty("NB_LINE.NAME", "Number of Line");
                internationalization.setProperty("SCHEMA.NAME", "Schema");
                doWrite(componentRoot, name + "_java.xml", stream -> {
                    final Method listener = processor.getListener();
                    try {
                        toXml(modelContext, stream, toModel(name, processor, getDesignModel(processor).getInputFlows(),
                                getDesignModel(processor).getOutputFlows(), internationalization, container, configKeys));
                    } catch (final JAXBException e) {
                        throw new IllegalStateException(e);
                    }
                });
                doWrite(componentRoot, name + "_messages.properties", stream -> {
                    try {
                        internationalization.store(stream, name + " internationalization");
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
                final byte[] iconStream = Icons.findIcon(processor.getIcon(), downloadIconsFromGithub, svgRepository);
                if (iconStream != null) {
                    doWrite(componentRoot, name + "_icon32.png", stream -> {
                        try {
                            stream.write(iconStream);
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                }
                Stream.of("begin", "main", "finally").forEach(part -> doWrite(componentRoot, name + "_" + part + ".javajet",
                        freemarkers.templatize("processor_" + part, createContext("processor", processor, configKeys))));
            });
        });

        // create the zip based on workdir
        final String prefix = workDir.getAbsolutePath() + File.separator;
        final String[] children = workDir.list();
        if (children == null) {
            throw new MojoExecutionException("No component found in " + workDir);
        }
        try (final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new FileOutputStream(zip))) {
            for (final String entry : children) {
                zip(zos, new File(workDir, entry), prefix);
            }
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        if (attach) {
            mavenProjectHelper.attachArtifact(project, "zip", classifier, zip);
            getLog().info("Attached " + zip.getName());
        }
    }

    private void toXml(final JAXBContext modelContext, final OutputStream stream, final COMPONENT component)
            throws JAXBException {
        final Marshaller marshaller = modelContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(new JAXBElement<>(new QName("COMPONENT"), COMPONENT.class, component), stream);
    }

    private COMPONENT toModel(final String legacyName, final ComponentFamilyMeta.BaseMeta comp, final Collection<String> inputs,
            final Collection<String> outputs, final Properties internationalization, final Container container,
            final Collection<String> configKeys) {
        final boolean isProcessor = ComponentFamilyMeta.ProcessorMeta.class.isInstance(comp);
        final boolean isInput = ComponentFamilyMeta.PartitionMapperMeta.class.isInstance(comp);

        final COMPONENT component = new COMPONENT();
        {
            final HEADER header = new HEADER();
            header.setPLATEFORM("ALL");
            header.setSERIAL("");
            header.setVERSION(BigDecimal.valueOf(extractNumberFrom(version)));
            header.setSTATUS("ALPHA");
            header.setCOMPATIBILITY("ALL");
            header.setAUTHOR(author);
            header.setRELEASEDATE(new SimpleDateFormat("yyyyMMdd").format(new Date()));
            header.setSTARTABLE(inputs.isEmpty());
            header.setLOG4JENABLED(true);
            header.setSIGNATURE("");
            component.setHEADER(header);
        }
        {
            final FAMILIES families = new FAMILIES();
            final ComponentFamilyMeta parent = comp.getParent();
            families.getFAMILY().addAll(parent.getCategories().stream().map(c -> c + '/' + parent.getName()).collect(toSet()));
            component.setFAMILIES(families);
        }
        {
            final DOCUMENTATION documentation = new DOCUMENTATION();
            documentation.setURL("");
            component.setDOCUMENTATION(documentation);
        }
        {
            final CONNECTORS connectors = new CONNECTORS();
            if (inputs.isEmpty()) {
                {
                    final CONNECTOR connector = new CONNECTOR();
                    connector.setCTYPE("FLOW");
                    connector.setMAXINPUT(0);
                    connector.setMAXOUTPUT(1);
                    connectors.getCONNECTOR().add(connector);
                }
                /*
                 * {
                 * final CONNECTOR connector = new CONNECTOR();
                 * connector.setCTYPE("ITERATE");
                 * connector.setMAXINPUT(0);
                 * connector.setMAXOUTPUT(1);
                 * connectors.getCONNECTOR().add(connector);
                 * }
                 * {
                 * final CONNECTOR connector = new CONNECTOR();
                 * connector.setCTYPE("SUBJOB_OK");
                 * connector.setMAXINPUT(1);
                 * connectors.getCONNECTOR().add(connector);
                 * }
                 * {
                 * final CONNECTOR connector = new CONNECTOR();
                 * connector.setCTYPE("SUBJOB_ERROR");
                 * connector.setMAXINPUT(1);
                 * connectors.getCONNECTOR().add(connector);
                 * }
                 * {
                 * final CONNECTOR connector = new CONNECTOR();
                 * connector.setCTYPE("COMPONENT_OK");
                 * connectors.getCONNECTOR().add(connector);
                 * }
                 * {
                 * final CONNECTOR connector = new CONNECTOR();
                 * connector.setCTYPE("COMPONENT_ERROR");
                 * connectors.getCONNECTOR().add(connector);
                 * }
                 * {
                 * final CONNECTOR connector = new CONNECTOR();
                 * connector.setCTYPE("RUN_IF");
                 * connectors.getCONNECTOR().add(connector);
                 * }
                 */
            } else { // TBD
                final CONNECTOR connector = new CONNECTOR();
                connector.setCTYPE("FLOW");
                connector.setMAXINPUT(inputs.size());
                connector.setMAXOUTPUT(outputs.size());
                connectors.getCONNECTOR().add(connector);
            }
            component.setCONNECTORS(connectors);
        }
        {
            final PARAMETERS parameters = new PARAMETERS();
            if (isInput || (isProcessor && hasOutputs(ComponentFamilyMeta.ProcessorMeta.class.cast(comp).getType()))) {
                final COLUMN column = new COLUMN();
                column.setNAME("item");
                column.setTYPE("id_Object");

                final TABLE table = new TABLE();
                table.getCOLUMN().add(column);

                final PARAMETER parameter = new PARAMETER();
                parameter.setNAME("SCHEMA");
                parameter.setFIELD("SCHEMA_TYPE");
                parameter.setNUMROW(1);
                parameter.getTABLE().add(table);
                parameters.getPARAMETER().add(parameter);
            }
            if (isProcessor) {
                final DEFAULT aDefault = new DEFAULT();
                aDefault.setValue("10");

                final PARAMETER parameter = new PARAMETER();
                parameter.setNAME("CHUNK_SIZE");
                parameter.setFIELD("id_Integer");
                parameter.setNUMROW(2);
                parameter.getDEFAULT().add(aDefault);
                parameters.getPARAMETER().add(parameter);
                internationalization.put("CHUNK_SIZE.NAME", "Chunk Size");
            }
            /*
             * comp.getParameterMetas().stream().filter(p -> p.getMetadata().containsKey("dataset")).findFirst()
             * .ifPresent(datasetParam -> {
             * final PARAMETER parameter = new PARAMETER();
             * parameter.setNAME("GUESS_SCHEMA");
             * parameter.setFIELD("GUESS_SCHEMA"); // ad-hoc for SQL only :(
             * parameter.setNUMROW(2);
             * parameters.getPARAMETER().add(parameter);
             * });
             */

            parameters.getPARAMETER().addAll(asParameters(emptyMap(), comp.getParameterMetas(), container, internationalization,
                    new AtomicInteger(10), configKeys));
            component.setPARAMETERS(parameters);
        }
        {
            final IMPORTS imports = new IMPORTS();
            imports.getIMPORTOrIMPORTS().addAll(MANDATORY_DEPENDENCIES);

            if (defineDependencies) { // dependencies - note: we would need a flag to say "package but don't put in classpath"
                imports.getIMPORTOrIMPORTS()
                        .addAll(Stream.concat(
                                Stream.of(new DefaultArtifact(groupId, artifactId, version, "compile", packaging, null,
                                        new DefaultArtifactHandler())),
                                project.getArtifacts().stream()
                                        .filter(a -> "compile".equals(a.getScope()) || "runtime".equals(a.getScope())))
                                .map(a -> {
                                    final boolean hasClassifier = a.getClassifier() != null && !a.getClassifier().isEmpty();
                                    return newImport(a.getArtifactId(),
                                            "mvn:" + a.getGroupId() + '/' + a.getArtifactId() + '/' + a.getVersion()
                                                    + (!"jar".equals(a.getType()) || hasClassifier
                                                            ? '/' + a.getType() + (hasClassifier ? '/' + a.getClassifier() : "")
                                                            : ""),
                                            null);
                                }).collect(toList()));
            }

            final CODEGENERATION codeGeneration = new CODEGENERATION();
            codeGeneration.setIMPORTS(imports);
            component.setCODEGENERATION(codeGeneration);
        }
        {
            final RETURNS returns = new RETURNS();
            {
                final RETURN nbLine = new RETURN();
                nbLine.setNAME("NB_LINE");
                nbLine.setTYPE("id_Integer");
                nbLine.setAVAILABILITY("AFTER");
                returns.getRETURN().add(nbLine);
            }
            component.setRETURNS(returns);
        }

        {
            internationalization.setProperty("HELP", "org.talend.help." + legacyName);
            internationalization.setProperty("LONG_NAME",
                    comp.findBundle(container.getLoader(), Locale.ENGLISH).displayName().orElse(comp.getName()));
        }

        return component;
    }

    private boolean hasOutputs(final Class<?> type) {
        return Stream.of(type.getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class))
                .map(m -> m.getReturnType() != void.class
                        || Stream.of(m.getParameters()).anyMatch(p -> p.isAnnotationPresent(Output.class)))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Wrong processor " + type));
    }

    private Collection<PARAMETER> asParameters(final Map<String, String> rootParams, final Collection<ParameterMeta> metas,
            final Container container, final Properties internationalization, final AtomicInteger numCounter,
            final Collection<String> configKeys) {
        final String order = rootParams.get("tcomp::ui::optionsorder::value");
        final List<String> paramOrder = order == null ? emptyList() : asList(order.split(","));
        return metas.stream().sorted((o1, o2) -> {
            final int order1 = paramOrder.indexOf(o1.getName());
            final int order2 = paramOrder.indexOf(o2.getName());
            if (order1 != order2) {
                return order1 - order2;
            }
            return o1.getName().compareTo(o2.getName());
        }).flatMap(param -> {
            final boolean skip = param.getType() == ParameterMeta.Type.OBJECT;
            final int numRow = skip ? -1 : numCounter.incrementAndGet();
            final Stream<PARAMETER> nested = asParameters(param.getMetadata(), param.getNestedParameters(), container,
                    internationalization, numCounter, configKeys).stream();
            if (skip) {
                return nested;
            }

            if (param.getType() == ParameterMeta.Type.ARRAY) {
                //
                // todo: handle list as a table of nested?
                //
                throw new IllegalArgumentException("ARRAY not yet supported: " + param);
            }

            final PARAMETER parameter = new PARAMETER();
            final String name = toParamName(param.getPath());
            parameter.setNAME(name); // ensure it is a java name
            parameter.setNUMROW(numRow);
            parameter.setREQUIRED("true".equals(param.getMetadata().get("tcomp::validation::required")));

            // filter
            // show_if
            // dynamic setting
            // default
            // repository value

            switch (param.getType()) {
            case BOOLEAN:
                parameter.setFIELD("CHECK");
                break;
            case ARRAY:
                parameter.setFIELD("OPENED_LIST");
                break;
            case ENUM: {
                parameter.setFIELD("CLOSED_LIST");

                final ITEMS items = new ITEMS();
                items.setDEFAULT(param.getProposals().isEmpty() ? null : param.getProposals().iterator().next());
                items.getITEM().addAll(param.getProposals().stream().sorted().map(val -> {
                    final ITEM item = new ITEM();
                    item.setNAME(val);
                    item.setVALUE(val);
                    return item;
                }).collect(toList()));
                parameter.setITEMS(items);
                break;
            }
            case NUMBER:
            case STRING:
                if ("true".equals(param.getMetadata().get("tcomp::ui::credential"))) {
                    parameter.setFIELD("PASSWORD");
                    configKeys.add(parameter.getNAME());
                } else {
                    final String codeType = param.getMetadata().get("tcomp::code::value");
                    if ("perl".equals(codeType)) {
                        parameter.setFIELD("MEMO_PERL");
                    } else if ("sql".equals(codeType)) {
                        parameter.setFIELD("MEMO_SQL");
                    } else if (codeType != null) {
                        parameter.setFIELD("MEMO_JAVA");
                    } else {
                        parameter.setFIELD("TEXT");
                    }
                }
                break;
            default:
                parameter.setFIELD("TEXT");
                break;
            }

            final String defaultValue = param.getMetadata().get("tcomp::ui::defaultvalue::value");
            if (defaultValue != null) {
                final DEFAULT def = new DEFAULT();
                def.setValue(defaultValue);
                parameter.getDEFAULT().add(def);
            }

            // only handle EN as default for now
            internationalization.setProperty(name + ".NAME",
                    param.findBundle(container.getLoader(), Locale.ENGLISH).displayName().orElse(param.getName()));

            configKeys.add(parameter.getNAME());
            return Stream.concat(Stream.of(parameter), nested);
        }).collect(toList());
    }

    private String toParamName(final String param) {
        return param.replace('.', '$');
    }

    private String toName(final ComponentFamilyMeta.BaseMeta mapper) {
        return "t" + capitaliseAllWords(mapper.getParent().getName()) + capitaliseAllWords(mapper.getName());
    }

    private double extractNumberFrom(final String version) {
        final int firstDot = version.indexOf('.');
        if (firstDot < 0) {
            return 1.0;
        }
        int secondDot = version.indexOf('.', firstDot + 1);
        if (secondDot < 0) {
            secondDot = version.indexOf('-', firstDot + 1);
            if (secondDot < 0) {
                return Double.parseDouble(version.substring(0, firstDot));
            }
        }
        return Double.parseDouble(version.substring(0, secondDot));
    }

    private void doWrite(final File compFolder, final String name, final Consumer<OutputStream> worker) {
        try (final OutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(compFolder, name)))) {
            worker.accept(stream);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void doWrite(final File compFolder, final String name, final String value) {
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

    private void zip(final ZipArchiveOutputStream zip, final File f, final String prefix) throws IOException {
        final String path = f.getPath().replace(prefix, "").replace(File.separator, "/");
        final ZipArchiveEntry archiveEntry = new ZipArchiveEntry(f, path);
        zip.putArchiveEntry(archiveEntry);
        if (f.isDirectory()) {
            zip.closeArchiveEntry();
            final File[] files = f.listFiles();
            if (files != null) {
                for (final File child : files) {
                    zip(zip, child, prefix);
                }
            }
        } else {
            Files.copy(f.toPath(), zip);
            zip.closeArchiveEntry();
        }
    }

    private static IMPORT newImport(final String name, final String mvn, final String urlPath) {
        final String usedName = name + (!name.endsWith(".jar") ? '-' + mvn.substring(mvn.lastIndexOf('/') + 1) + ".jar" : "");
        final IMPORT mvnImport = new IMPORT();
        mvnImport.setREQUIRED(true);
        mvnImport.setNAME(usedName);
        mvnImport.setMODULE(usedName);
        mvnImport.setMVN(mvn);
        mvnImport.setUrlPath(urlPath);
        return mvnImport;
    }

    private Map<String, Object> createContext(final String key, final Object instance, final Collection<String> configKeys) {
        final Map<String, Object> context = new HashMap<>();
        context.put(key, instance);
        context.put("configurationKeys", configKeys);
        return context;
    }
}
