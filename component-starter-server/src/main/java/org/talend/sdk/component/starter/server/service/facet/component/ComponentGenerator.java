/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.facet.component;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.talend.sdk.component.starter.server.configuration.StarterConfiguration;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.facet.util.NameConventions;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.AllArgsConstructor;
import lombok.Data;

@ApplicationScoped
public class ComponentGenerator {

    private final Comparator<Connection> connectionComparator = (o1, o2) -> {
        if ("__default__".equals(o1.getName())) {
            return -1;
        }

        if ("__default__".equals(o2.getName())) {
            return 1;
        }

        return o1.getName().compareTo(o2.getName());
    };

    @Inject
    private StarterConfiguration config;

    @Inject
    private TemplateRenderer tpl;

    @Inject
    private NameConventions names;

    private byte[] defaultIconContent;

    private static boolean isOutput(final ProjectRequest.ProcessorConfiguration p) {
        return p.getOutputStructures() == null || p.getOutputStructures().isEmpty();
    }

    private static boolean isProcessor(final ProjectRequest.ProcessorConfiguration p) {
        return !isOutput(p);
    }

    @PostConstruct
    private void init() {
        defaultIconContent = new byte[0];
    }

    public Stream<FacetGenerator.InMemoryFile> create(final String packageBase, final Build build, final String family,
            final String category, final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors) {
        final String mainJava = build.getMainJavaDirectory() + '/' + packageBase.replace('.', '/');
        final Map<String, Map<String, String>> messageProperties = new HashMap<>();// Package , list of configuration
                                                                                   // path for that package
        messageProperties.put(packageBase, new TreeMap<>());
        if (family != null && !family.isEmpty()) {
            messageProperties.get(packageBase).put(family, family);
        }

        final boolean hasService =
                (sources != null && !sources.isEmpty()) || (processors != null && !processors.isEmpty());
        if (!hasService) {
            return Stream.empty();
        }

        final String serviceName = names.toJavaName(build.getArtifact()) + "Service";
        final String usedFamily = ofNullable(family).orElse(build.getArtifact());

        final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();
        files.add(new FacetGenerator.InMemoryFile(mainJava + "/package-info.java",
                tpl.render("generator/component/package-info.mustache", new HashMap<String, Object>() {

                    {
                        put("package", packageBase);
                        put("family", usedFamily);
                        put("category", ofNullable(category).orElse(build.getArtifact()));
                    }
                })));
        files.add(new FacetGenerator.InMemoryFile(
                build.getMainResourcesDirectory() + "/icons/" + usedFamily + "_icon32.png", defaultIconContent));
        files.add(new FacetGenerator.InMemoryFile(mainJava + "/service/" + serviceName + ".java",
                tpl.render("generator/component/Service.mustache", new HashMap<String, Object>() {

                    {
                        put("className", serviceName);
                        put("package", packageBase + ".service");
                    }
                })));

        if (sources != null && !sources.isEmpty()) {
            files.addAll(createSourceFiles(packageBase, sources, mainJava, serviceName).collect(toList()));

            messageProperties.put(packageBase + ".source", new TreeMap<String, String>() {

                {
                    putAll(sources
                            .stream()
                            .map(source -> Pair.of(family + "." + source.getName(), source.getName()))
                            .collect(toMap(Pair::getKey, Pair::getValue)));

                    put("configuration", "configuration");
                    putAll(sources
                            .stream()
                            .filter(source -> source.getConfiguration() != null
                                    && source.getConfiguration().getEntries() != null)
                            .flatMap(source -> toProperties(packageBase + ".source",
                                    source.getConfiguration().getEntries(), null))
                            .collect(toMap(Pair::getKey, Pair::getValue)));
                }
            });
        }

        if (processors != null && !processors.isEmpty()) {
            files.addAll(createProcessorFiles(packageBase, processors, mainJava, serviceName).collect(toList()));
            messageProperties.put(packageBase + ".output", new TreeMap<String, String>() {

                {
                    putAll(processors
                            .stream()
                            .filter(ComponentGenerator::isOutput)
                            .map(processor -> Pair.of(family + "." + processor.getName(), processor.getName()))
                            .collect(toMap(Pair::getKey, Pair::getValue)));
                    put("configuration", "configuration");
                    putAll(processors
                            .stream()
                            .filter(processor -> processor.getConfiguration() != null
                                    && processor.getConfiguration().getEntries() != null)
                            .filter(ComponentGenerator::isOutput)
                            .flatMap(
                                    p -> toProperties(packageBase + ".output", p.getConfiguration().getEntries(), null))
                            .collect(toMap(Pair::getKey, Pair::getValue)));
                }
            });

            messageProperties.put(packageBase + ".processor", new TreeMap<String, String>() {

                {
                    put("configuration", "configuration");
                    putAll(processors
                            .stream()
                            .filter(ComponentGenerator::isProcessor)
                            .map(processor -> Pair.of(family + "." + processor.getName(), processor.getName()))
                            .collect(toMap(Pair::getKey, Pair::getValue)));
                    putAll(processors
                            .stream()
                            .filter(processor -> processor.getConfiguration() != null
                                    && processor.getConfiguration().getEntries() != null)
                            .filter(ComponentGenerator::isProcessor)
                            .flatMap(p -> toProperties(packageBase + ".processor", p.getConfiguration().getEntries(),
                                    null))
                            .collect(toMap(Pair::getKey, Pair::getValue)));
                }
            });
        }

        files.addAll(generateProperties(build.getMainResourcesDirectory(), messageProperties).collect(toList()));

        return files.stream();
    }

    private Stream<Pair<String, String>> toProperties(final String pck,
            final Collection<ProjectRequest.Entry> structure, final String parentPath) {
        return structure.stream().flatMap(e -> {
            final String prop = (parentPath == null || parentPath.isEmpty()) ? "configuration." + e.getName()
                    : parentPath + "." + e.getName();
            final Pair<String, String> pair = Pair.of(prop, e.getName());
            if (e.getNestedType() != null) {
                return Stream.concat(Stream.of(pair), toProperties(pck, e.getNestedType().getEntries(), prop));
            }
            return Stream.of(pair);
        });
    }

    private Stream<FacetGenerator.InMemoryFile> generateProperties(final String mainResourcesDirectory,
            final Map<String, Map<String, String>> messageProperties) {

        return messageProperties
                .entrySet()
                .stream()
                .map(props -> new FacetGenerator.InMemoryFile(
                        mainResourcesDirectory + "/" + props.getKey().replace(".", "/") + "/Messages.properties",
                        tpl.render("generator/component/Messages.mustache", new HashMap<String, Object>() {

                            {
                                put("properties", props.getValue().entrySet());
                            }
                        })));
    }

    private Stream<FacetGenerator.InMemoryFile> createProcessorFiles(final String packageBase,
            final Collection<ProjectRequest.ProcessorConfiguration> processors, final String mainJava,
            final String serviceName) {
        return processors.stream().flatMap(processor -> {
            final boolean isOutput =
                    processor.getOutputStructures() == null || processor.getOutputStructures().isEmpty();

            final String className = names.toProcessorName(processor);
            final String configurationClassName = names.toConfigurationName(className);
            final String processorFinalPackage = isOutput ? "output" : "processor";
            final String processorPackage = packageBase + "." + processorFinalPackage;

            final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();

            final List<Connection> outputNames =
                    !isOutput ? processor.getOutputStructures().entrySet().stream().map(e -> {
                        final String javaName = names.sanitizeConnectionName(e.getKey());
                        if (e.getValue().isGeneric()) {
                            return new Connection(e.getKey(), javaName, "ObjectMap", isDefault(e.getKey()));
                        }

                        final String outputClassName =
                                capitalize(processor.getName() + capitalize(javaName + "Output"));
                        generateModel(null, processorPackage, mainJava, e.getValue().getStructure(), outputClassName,
                                files);
                        return new Connection(e.getKey(), javaName, outputClassName, isDefault(e.getKey()));
                    }).collect(toList()) : emptyList();
            Collections.sort(outputNames, connectionComparator);

            final List<Connection> inputNames = processor.getInputStructures() != null
                    ? processor.getInputStructures().entrySet().stream().map(e -> {
                        final String javaName = names.sanitizeConnectionName(e.getKey());
                        if (e.getValue().isGeneric()) {
                            return new Connection(e.getKey(), javaName, "ObjectMap", isDefault(e.getKey()));
                        }

                        final String inputClassName = capitalize(processor.getName() + capitalize(javaName + "Input"));
                        generateModel(null, processorPackage, mainJava, e.getValue().getStructure(), inputClassName,
                                files);
                        return new Connection(e.getKey(), javaName, inputClassName, isDefault(e.getKey()));
                    }).collect(toList())
                    : emptyList();
            Collections.sort(inputNames, connectionComparator);

            generateConfiguration(null, processorPackage, mainJava, processor.getConfiguration(),
                    configurationClassName, files);

            files.add(
                    new FacetGenerator.InMemoryFile(mainJava + "/" + processorFinalPackage + "/" + className + ".java",
                            tpl.render("generator/component/Processor.mustache", new HashMap<String, Object>() {

                                {
                                    put("name", processor.getName());
                                    put("className", className);
                                    put("package", packageBase + "." + processorFinalPackage);
                                    put("serviceName", serviceName);
                                    put("servicePackage", packageBase + ".service");
                                    put("configurationName", configurationClassName);
                                    put("inputs", inputNames);
                                    put("outputs", outputNames);
                                    put("hasOutputs", outputNames.size() != 0);
                                    put("icon", ofNullable(processor.getIcon()).filter(s -> !s.isEmpty()).orElse(
                                            "Icon.IconType.STAR"));
                                    put("generic", outputNames.stream().anyMatch(o -> o.type.equals("ObjectMap"))
                                            || inputNames.stream().anyMatch(o -> o.type.equals("ObjectMap")));
                                }
                            })));

            return files.stream();
        });
    }

    private boolean isDefault(final String name) {
        return "__default__".equals(name);
    }

    private Stream<FacetGenerator.InMemoryFile> createSourceFiles(final String packageBase,
            final Collection<ProjectRequest.SourceConfiguration> sources, final String mainJava,
            final String serviceName) {
        return sources.stream().flatMap(source -> {
            final boolean generic = source.getOutputStructure() == null || source.getOutputStructure().isGeneric()
                    || source.getOutputStructure().getStructure() == null;

            final String baseName = names.toJavaName(source.getName());
            final String sourceName = names.toSourceName(baseName);
            final String mapperName = names.toMapperName(baseName);
            final String configurationClassName = names.toConfigurationName(mapperName);
            final String modelClassName = names.toMapperRecordName(source);
            final String sourcePackage = packageBase + ".source";

            final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();
            files.add(new FacetGenerator.InMemoryFile(mainJava + "/source/" + mapperName + ".java",
                    tpl.render("generator/component/Mapper.mustache", new HashMap<String, Object>() {

                        {
                            put("name", source.getName());
                            put("className", mapperName);
                            put("package", sourcePackage);
                            put("serviceName", serviceName);
                            put("servicePackage", packageBase + ".service");
                            put("configurationName", configurationClassName);
                            put("sourceName", sourceName);
                            put("infinite", source.isStream());
                            put("icon", ofNullable(source.getIcon()).filter(s -> !s.isEmpty()).orElse(
                                    "Icon.IconType.STAR"));
                        }
                    })));
            files.add(new FacetGenerator.InMemoryFile(mainJava + "/source/" + sourceName + ".java",
                    tpl.render("generator/component/Source.mustache", new HashMap<String, Object>() {

                        {
                            put("className", sourceName);
                            put("package", sourcePackage);
                            put("serviceName", serviceName);
                            put("servicePackage", packageBase + ".service");
                            put("configurationName", configurationClassName);
                            put("modelName", modelClassName);
                            put("generic", generic);
                        }
                    })));
            generateConfiguration(null, sourcePackage, mainJava, source.getConfiguration(), configurationClassName,
                    files);
            generateModel(null, sourcePackage, mainJava, generic ? null : source.getOutputStructure().getStructure(),
                    modelClassName, files);
            return files.stream();
        });
    }

    private void generateModel(final String root, final String packageBase, final String mainJava,
            final ProjectRequest.DataStructure structure, final String modelClassName,
            final Collection<FacetGenerator.InMemoryFile> files) {
        files
                .add(new FacetGenerator.InMemoryFile(
                        mainJava + "/" + packageBase.substring(packageBase.lastIndexOf('.') + 1) + "/" + modelClassName
                                + ".java",
                        tpl.render("generator/component/Model.mustache", new HashMap<String, Object>() {

                            {
                                put("className", modelClassName);
                                put("package", packageBase);
                                put("generic", structure == null);
                                if (structure != null && structure.getEntries() != null) {
                                    put("structure", structure
                                            .getEntries()
                                            .stream()
                                            .map(e -> new Property(e.getName(), capitalize(e.getName()),
                                                    names.toJavaConfigType(root, packageBase, e, (fqn, nested) -> {
                                                        final int li = fqn.lastIndexOf('.');
                                                        final String pck = li > 0 ? fqn.substring(0, li) : "";
                                                        final String cn = li > 0 ? fqn.substring(li + 1) : fqn;
                                                        generateModel((root == null ? "" : root) + capitalize(cn), pck,
                                                                mainJava, e.getNestedType(), cn, files);
                                                    }), false))
                                            .collect(toList()));
                                }
                            }
                        })));
    }

    private void generateConfiguration(final String root, final String packageBase, final String mainJava,
            final ProjectRequest.DataStructure structure, final String configurationClassName,
            final Collection<FacetGenerator.InMemoryFile> files) {
        files.add(new FacetGenerator.InMemoryFile(
                mainJava + "/" + packageBase.substring(packageBase.lastIndexOf('.') + 1) + "/" + configurationClassName
                        + ".java",
                tpl.render("generator/component/Configuration.mustache", new HashMap<String, Object>() {

                    {

                        List<Property> structures = structure != null && structure.getEntries() != null ? structure
                                .getEntries()
                                .stream()
                                .map(e -> new Property(e.getName(), capitalize(e.getName()),
                                        names.toJavaConfigType(root, packageBase, e, (fqn, nested) -> {
                                            final int li = fqn.lastIndexOf('.');
                                            final String pck = li > 0 ? fqn.substring(0, li) : "";
                                            final String cn = li > 0 ? fqn.substring(li + 1) : fqn;
                                            generateConfiguration((root == null ? "" : root) + capitalize(cn), pck,
                                                    mainJava, nested, cn, files);
                                        }), isCredential(e.getName(), e.getType())))
                                .collect(toList()) : emptyList();

                        put("className", configurationClassName);
                        put("package", packageBase);
                        put("structure", structures);
                        put("hasCredential", structures.stream().anyMatch(s -> s.isCredential));

                    }
                })));
    }

    private boolean isCredential(final String name, final String type) {
        return name != null && !name.isEmpty() && "string".equals(type)
                && config.dictionaryCredentials().stream().anyMatch(name::contains);
    }

    @Data
    @AllArgsConstructor
    public static class Property {

        private final String name;

        private final String methodName;

        private final String type;

        private final boolean isCredential;
    }

    @Data
    @AllArgsConstructor
    public static class Connection {

        private final String name;

        private final String javaName;

        private final String type;

        private final boolean isDefault;
    }
}
