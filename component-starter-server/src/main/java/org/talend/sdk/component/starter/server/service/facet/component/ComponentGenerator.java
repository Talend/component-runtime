/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.starter.server.service.Strings.capitalize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

        final String baseName = names.toJavaName(build.getArtifact());
        final String serviceName = baseName + "Service";
        final String usedFamily = ofNullable(family).orElse(build.getArtifact());

        final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();
        files
                .add(new FacetGenerator.InMemoryFile(mainJava + "/package-info.java",
                        tpl.render("generator/component/package-info.mustache", new HashMap<String, Object>() {

                            {
                                put("package", packageBase);
                                put("family", usedFamily);
                                put("category", ofNullable(category).orElse(build.getArtifact()));
                            }
                        })));
        files
                .add(new FacetGenerator.InMemoryFile(
                        build.getMainResourcesDirectory() + "/icons/" + usedFamily + "_icon32.png",
                        defaultIconContent));
        files
                .add(new FacetGenerator.InMemoryFile(mainJava + "/service/" + serviceName + ".java",
                        tpl.render("generator/component/Service.mustache", new HashMap<String, Object>() {

                            {
                                put("className", serviceName);
                                put("package", packageBase + ".service");
                            }
                        })));

        final String configurationsPackageName = packageBase + ".configuration";
        final boolean hasConfigurations = (sources != null && !sources.isEmpty())
                || (processors != null && processors.stream().anyMatch(ComponentGenerator::isOutput));
        final String datasetName;
        if (hasConfigurations) {
            final String basePath = mainJava + "/"
                    + configurationsPackageName.substring(configurationsPackageName.lastIndexOf('.') + 1) + "/";
            final Map<String, Object> templateVariables = new HashMap<String, Object>() {

                {
                    put("baseName", baseName);
                    put("package", configurationsPackageName);
                }
            };
            files
                    .addAll(Stream
                            .of("DataSet", "DataStore")
                            .map(it -> new FacetGenerator.InMemoryFile(basePath + baseName + it + ".java",
                                    tpl.render("generator/component/" + it + ".mustache", templateVariables)))
                            .collect(toList()));
            datasetName = baseName + "DataSet";

            messageProperties
                    .put(configurationsPackageName,
                            new TreeMap<>(singletonMap(baseName + "DataSet.connection", "Connection")));

            final Map<String, String> baseProperty = messageProperties.get(packageBase);
            final String capitalizedFamily = capitalize(family);
            baseProperty.put(family + ".dataset.default", capitalizedFamily + " DataSet");
            baseProperty.put(family + ".datastore.default", capitalizedFamily + " DataStore");
        } else {
            datasetName = null;
        }

        if (sources != null && !sources.isEmpty()) {
            files
                    .addAll(createSourceFiles(packageBase, sources, mainJava, serviceName, configurationsPackageName,
                            datasetName).collect(toList()));

            messageProperties.put(packageBase + ".source", new TreeMap<String, String>() {

                {
                    putAll(sources
                            .stream()
                            .map(source -> new StringTuple2(family + "." + source.getName(), source.getName()))
                            .collect(toMap(StringTuple2::getKey, StringTuple2::getValue)));
                    putAll(sources
                            .stream()
                            .filter(source -> source.getConfiguration() != null
                                    && source.getConfiguration().getEntries() != null)
                            .flatMap(source -> toProperties(names.toMapperName(source.getName()),
                                    source.getConfiguration().getEntries()))
                            .collect(toMap(StringTuple2::getKey, StringTuple2::getValue, (k1, k2) -> k1)));
                }
            });
        }

        if (processors != null && !processors.isEmpty()) {
            files
                    .addAll(createProcessorFiles(packageBase, processors, mainJava, serviceName,
                            configurationsPackageName, datasetName).collect(toList()));
            messageProperties.put(packageBase + ".output", new TreeMap<String, String>() {

                {
                    putAll(processors
                            .stream()
                            .filter(ComponentGenerator::isOutput)
                            .map(processor -> new StringTuple2(family + "." + processor.getName(), processor.getName()))
                            .collect(toMap(StringTuple2::getKey, StringTuple2::getValue)));
                    putAll(processors
                            .stream()
                            .filter(processor -> processor.getConfiguration() != null
                                    && processor.getConfiguration().getEntries() != null)
                            .filter(ComponentGenerator::isOutput)
                            .flatMap(p -> toProperties(names.toProcessorName(p), p.getConfiguration().getEntries()))
                            .collect(toMap(StringTuple2::getKey, StringTuple2::getValue, (k1, k2) -> k1)));
                }
            });

            messageProperties.put(packageBase + ".processor", new TreeMap<String, String>() {

                {
                    putAll(processors
                            .stream()
                            .filter(ComponentGenerator::isProcessor)
                            .map(processor -> new StringTuple2(family + "." + processor.getName(), processor.getName()))
                            .collect(toMap(StringTuple2::getKey, StringTuple2::getValue)));
                    putAll(processors
                            .stream()
                            .filter(processor -> processor.getConfiguration() != null
                                    && processor.getConfiguration().getEntries() != null)
                            .filter(ComponentGenerator::isProcessor)
                            .flatMap(p -> toProperties(names.toProcessorName(p), p.getConfiguration().getEntries()))
                            .collect(toMap(StringTuple2::getKey, StringTuple2::getValue, (k1, k2) -> k1)));
                }
            });
        }
        files.addAll(generateProperties(build.getMainResourcesDirectory(), messageProperties).collect(toList()));
        return files.stream();
    }

    private void ensureConfigurationHasDataSet(final ProjectRequest.DataStructure it, final String datasetTypeName) {
        it
                .getEntries()
                .add(0, new ProjectRequest.Entry(findNotUsedNameForDataSet(it.getEntries()), datasetTypeName, null));
    }

    private String findNotUsedNameForDataSet(final Collection<ProjectRequest.Entry> entries) {
        // first try dataset
        return Stream
                .of("dataset", "dataSet", "configurationDataset", "configurationDataSet", "_dataset")
                .filter(it -> entries.stream().noneMatch(e -> it.equals(e.getName())))
                .findFirst()
                .orElseGet(() -> { // sequence logic to ensure we match something
                    int current = 1;
                    final AtomicReference<String> name = new AtomicReference<>();
                    do {
                        name.set("dataset" + current);
                        current++;
                    } while (entries.stream().anyMatch(e -> name.get().equals(e.getName())));
                    return name.get();
                });
    }

    private Stream<StringTuple2> toProperties(final String componentName,
            final Collection<ProjectRequest.Entry> structure) {
        return structure.stream().flatMap(e -> {
            final String prop = names.toConfigurationName(componentName) + "." + e.getName();

            final StringTuple2 pair = new StringTuple2(prop, e.getName());
            if (e.getNestedType() != null) {
                return Stream.concat(Stream.of(pair), toProperties(e.getName(), e.getNestedType().getEntries()));
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
            final String serviceName, final String datasetPackage, final String datasetName) {
        return processors.stream().flatMap(processor -> {
            final boolean isOutput = isOutput(processor);
            if (isOutput) {
                ensureConfigurationHasDataSet(processor.getConfiguration(), datasetName);
            }

            final String className = names.toProcessorName(processor);
            final String configurationClassName = names.toConfigurationName(className);
            final String processorFinalPackage = isOutput ? "output" : "processor";
            final String processorPackage = packageBase + "." + processorFinalPackage;

            final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();

            final List<Connection> outputNames =
                    !isOutput ? processor.getOutputStructures().entrySet().stream().map(e -> {
                        final String javaName = names.sanitizeConnectionName(e.getKey());
                        if (e.getValue().isGeneric()) {
                            return new Connection(e.getKey(), javaName, "Record", isDefault(e.getKey()));
                        }

                        final String outputClassName =
                                capitalize(processor.getName() + capitalize(javaName + "Output"));
                        generateModel(null, processorPackage, mainJava, e.getValue().getStructure(), outputClassName,
                                files);
                        return new Connection(e.getKey(), javaName, outputClassName, isDefault(e.getKey()));
                    }).sorted(connectionComparator).collect(toList()) : emptyList();

            final List<Connection> inputNames = processor.getInputStructures() != null
                    ? processor.getInputStructures().entrySet().stream().map(e -> {
                        final String javaName = names.sanitizeConnectionName(e.getKey());
                        if (e.getValue().isGeneric()) {
                            return new Connection(e.getKey(), javaName, "Record", isDefault(e.getKey()));
                        }

                        final String inputClassName = capitalize(processor.getName() + capitalize(javaName + "Input"));
                        generateModel(null, processorPackage, mainJava, e.getValue().getStructure(), inputClassName,
                                files);
                        return new Connection(e.getKey(), javaName, inputClassName, isDefault(e.getKey()));
                    }).sorted(connectionComparator).collect(toList())
                    : emptyList();

            generateConfiguration(null, processorPackage, mainJava, processor.getConfiguration(),
                    configurationClassName, files, datasetPackage, !isOutput ? null : datasetName);

            files
                    .add(new FacetGenerator.InMemoryFile(
                            mainJava + "/" + processorFinalPackage + "/" + className + ".java",
                            tpl.render("generator/component/Processor.mustache", new HashMap<String, Object>() {

                                {
                                    put("name", processor.getName());
                                    put("className", className);
                                    put("package", packageBase + "." + processorFinalPackage);
                                    put("serviceName", serviceName);
                                    put("servicePackage", packageBase + ".service");
                                    put("configurationName", configurationClassName);
                                    put("inputs", inputNames);
                                    put("hasInputs", inputNames.size() != 0);
                                    put("outputs", outputNames);
                                    put("hasOutputs", outputNames.size() != 0);
                                    put("icon",
                                            ofNullable(processor.getIcon())
                                                    .filter(s -> !s.isEmpty())
                                                    .orElse("Icon.IconType.STAR"));
                                    put("generic", outputNames.stream().anyMatch(o -> o.type.equals("Record"))
                                            || inputNames.stream().anyMatch(o -> o.type.equals("Record")));
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
            final String serviceName, final String datasetPackage, final String datasetName) {
        return sources.stream().flatMap(source -> {
            final boolean generic = source.getOutputStructure() == null || source.getOutputStructure().isGeneric()
                    || source.getOutputStructure().getStructure() == null;

            ensureConfigurationHasDataSet(source.getConfiguration(), datasetName);

            final String baseName = names.toJavaName(source.getName());
            final String sourceName = names.toSourceName(baseName);
            final String mapperName = names.toMapperName(baseName);
            final String configurationClassName = names.toConfigurationName(mapperName);
            final String modelClassName = names.toMapperRecordName(source);
            final String sourcePackage = packageBase + ".source";

            final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();
            files
                    .add(new FacetGenerator.InMemoryFile(mainJava + "/source/" + mapperName + ".java",
                            tpl.render("generator/component/Mapper.mustache", new HashMap<String, Object>() {

                                {
                                    put("generic", generic);
                                    put("name", source.getName());
                                    put("className", mapperName);
                                    put("package", sourcePackage);
                                    put("serviceName", serviceName);
                                    put("servicePackage", packageBase + ".service");
                                    put("configurationName", configurationClassName);
                                    put("sourceName", sourceName);
                                    put("infinite", source.isStream());
                                    put("icon",
                                            ofNullable(source.getIcon())
                                                    .filter(s -> !s.isEmpty())
                                                    .orElse("Icon.IconType.STAR"));
                                }
                            })));
            files
                    .add(new FacetGenerator.InMemoryFile(mainJava + "/source/" + sourceName + ".java",
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
                    files, datasetPackage, datasetName);
            generateModel(null, sourcePackage, mainJava, generic ? null : source.getOutputStructure().getStructure(),
                    modelClassName, files);
            return files.stream();
        });
    }

    private void generateModel(final String root, final String packageBase, final String mainJava,
            final ProjectRequest.DataStructure structure, final String modelClassName,
            final Collection<FacetGenerator.InMemoryFile> files) {
        if (structure != null) {
            files
                    .add(new FacetGenerator.InMemoryFile(
                            mainJava + "/" + packageBase.substring(packageBase.lastIndexOf('.') + 1) + "/"
                                    + modelClassName + ".java",
                            tpl.render("generator/component/Model.mustache", new HashMap<String, Object>() {

                                {
                                    put("className", modelClassName);
                                    put("package", packageBase);
                                    put("generic", false);
                                    if (structure.getEntries() != null) {
                                        put("structure", structure
                                                .getEntries()
                                                .stream()
                                                .map(e -> new Property(e.getName(), capitalize(e.getName()),
                                                        names.toJavaConfigType(root, packageBase, e, (fqn, nested) -> {
                                                            final int li = fqn.lastIndexOf('.');
                                                            final String pck = li > 0 ? fqn.substring(0, li) : "";
                                                            final String cn = li > 0 ? fqn.substring(li + 1) : fqn;
                                                            generateModel((root == null ? "" : root) + capitalize(cn),
                                                                    pck, mainJava, e.getNestedType(), cn, files);
                                                        }), false))
                                                .collect(toList()));
                                    }
                                }
                            })));
        }
    }

    private void generateConfiguration(final String root, final String packageBase, final String mainJava,
            final ProjectRequest.DataStructure structure, final String configurationClassName,
            final Collection<FacetGenerator.InMemoryFile> files, final String datasetPackage,
            final String datasetName) {
        files
                .add(new FacetGenerator.InMemoryFile(
                        mainJava + "/" + packageBase.substring(packageBase.lastIndexOf('.') + 1) + "/"
                                + configurationClassName + ".java",
                        tpl.render("generator/component/Configuration.mustache", new HashMap<String, Object>() {

                            {

                                List<Property> structures =
                                        structure != null && structure.getEntries() != null ? structure
                                                .getEntries()
                                                .stream()
                                                .map(e -> new Property(e.getName(), capitalize(e.getName()),
                                                        names.toJavaConfigType(root, packageBase, e, (fqn, nested) -> {
                                                            final int li = fqn.lastIndexOf('.');
                                                            final String pck = li > 0 ? fqn.substring(0, li) : "";
                                                            final String cn = li > 0 ? fqn.substring(li + 1) : fqn;
                                                            generateConfiguration(
                                                                    (root == null ? "" : root) + capitalize(cn), pck,
                                                                    mainJava, nested, cn, files, null, null);
                                                        }), isCredential(e.getName(), e.getType())))
                                                .collect(toList()) : emptyList();

                                put("className", configurationClassName);
                                put("package", packageBase);
                                put("structure", structures);
                                put("hasCredential", structures.stream().anyMatch(s -> s.isCredential));
                                ofNullable(datasetName).ifPresent(it -> {
                                    put("datasetPackage", datasetPackage);
                                    put("datasetName", it);
                                });
                            }
                        })));
    }

    private boolean isCredential(final String name, final String type) {
        return name != null && !name.isEmpty() && "string".equals(type)
                && config.getDictionaryCredentials().stream().anyMatch(name::contains);
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

    @Data
    public static class StringTuple2 {

        private final String key;

        private final String value;
    }
}
