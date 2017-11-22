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

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.talend.sdk.component.starter.server.service.facet.util.NameConventions.sanitizeConnectionName;
import static org.talend.sdk.component.starter.server.service.facet.util.NameConventions.toJavaConfigType;
import static org.talend.sdk.component.starter.server.service.facet.util.NameConventions.toJavaName;

@ApplicationScoped
public class ComponentGenerator {

    @Inject
    private TemplateRenderer tpl;

    private byte[] defaultIconContent;

    @PostConstruct
    private void init() {
        defaultIconContent = new byte[0];
    }

    public Stream<FacetGenerator.InMemoryFile> create(final String packageBase, final Build build, final String family,
            final String category, final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors) {
        final String mainJava = build.getMainJavaDirectory() + '/' + packageBase.replace('.', '/');

        final boolean hasService = (sources != null && !sources.isEmpty()) || (processors != null && !processors.isEmpty());
        if (!hasService) {
            return Stream.empty();
        }

        final String serviceName = toJavaName(build.getArtifact()) + "Service";
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
        files.add(new FacetGenerator.InMemoryFile(build.getMainResourcesDirectory() + "/icons/" + usedFamily + "_icon32.png",
                defaultIconContent));
        files.add(new FacetGenerator.InMemoryFile(mainJava + "/service/" + serviceName + ".java",
                tpl.render("generator/component/Service.mustache", new HashMap<String, Object>() {

                    {
                        put("className", serviceName);
                        put("package", packageBase + ".service");
                    }
                })));

        // boolean needsObjectMapImpl = false; // we have a default impl in api, no need to generate one
        if (sources != null && !sources.isEmpty()) {
            files.addAll(createSourceFiles(packageBase, sources, mainJava, serviceName).collect(toList()));
            // needsObjectMapImpl = sources.stream().anyMatch(ProjectRequest.SourceConfiguration::isGenericOutput);
        }
        if (processors != null && !processors.isEmpty()) {
            files.addAll(createProcessorFiles(packageBase, processors, mainJava, serviceName).collect(toList()));
            /*
             * if (!needsObjectMapImpl) {
             * needsObjectMapImpl = processors.stream().anyMatch(ProjectRequest.ProcessorConfiguration::isGenericOutputs);
             * }
             */
        }

        return files.stream();
    }

    private Stream<FacetGenerator.InMemoryFile> createProcessorFiles(final String packageBase,
            final Collection<ProjectRequest.ProcessorConfiguration> processors,
            final String mainJava, final String serviceName) {
        return processors.stream().flatMap(processor -> {
            final boolean isOutput = processor.getOutputStructures() == null || processor.getOutputStructures().isEmpty();

            final String baseName = toJavaName(processor.getName());
            final String className = baseName + "Processor";
            final String configurationClassName = className + "Configuration";
            final String processorFinalPackage = isOutput ? "output" : "processor";
            final String processorPackage = packageBase + "." + processorFinalPackage;

            final Collection<FacetGenerator.InMemoryFile> files = new ArrayList<>();

            final Set<Connection> outputNames = !isOutput ? processor.getOutputStructures().entrySet().stream().map(e -> {
                final String javaName = sanitizeConnectionName(e.getKey());
                if (e.getValue().isGeneric()) {
                    return new Connection(e.getKey(), javaName, "ObjectMap");
                }

                final String outputClassName = capitalize(processor.getName() + capitalize(javaName + "Output"));
                generateModel(null, processorPackage, mainJava, e.getValue().getStructure(), outputClassName, files);
                return new Connection(e.getKey(), javaName, outputClassName);
            }).collect(toSet()) : emptySet();

            final Set<Connection> inputNames = processor.getInputStructures() != null
                    ? processor.getInputStructures().entrySet().stream().map(e -> {
                final String javaName = sanitizeConnectionName(e.getKey());
                if (e.getValue().isGeneric()) {
                    return new Connection(e.getKey(), javaName, "ObjectMap");
                }

                final String inputClassName = capitalize(processor.getName() + capitalize(javaName + "Input"));
                generateModel(null, processorPackage, mainJava, e.getValue().getStructure(), inputClassName, files);
                return new Connection(e.getKey(), javaName, inputClassName);
            }).collect(toSet()) : emptySet();

            generateConfiguration(null, processorPackage, mainJava, processor.getConfiguration(), configurationClassName, files);

            files.add(new FacetGenerator.InMemoryFile(mainJava + "/" + processorFinalPackage + "/" + className + ".java",
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
                            put("icon", ofNullable(processor.getIcon()).filter(s -> !s.isEmpty()).orElse("Icon.IconType.STAR"));
                            put("generic", outputNames.stream().anyMatch(o -> o.type.equals("ObjectMap"))
                                    || inputNames.stream().anyMatch(o -> o.type.equals("ObjectMap")));
                        }
                    })));

            return files.stream();
        });
    }

    private Stream<FacetGenerator.InMemoryFile> createSourceFiles(final String packageBase,
            final Collection<ProjectRequest.SourceConfiguration> sources, final String mainJava, final String serviceName) {
        return sources.stream().flatMap(source -> {
            final boolean generic = source.getOutputStructure() == null || source.getOutputStructure().isGeneric()
                    || source.getOutputStructure().getStructure() == null;

            final String baseName = toJavaName(source.getName());
            final String sourceName = baseName + "Source";
            final String mapperName = baseName + "Mapper";
            final String configurationClassName = sourceName + "Configuration";
            final String modelClassName = generic ? baseName + "GenericRecord" : (baseName + "Record");
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
                            put("icon", ofNullable(source.getIcon()).filter(s -> !s.isEmpty()).orElse("Icon.IconType.STAR"));
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
            generateConfiguration(null, sourcePackage, mainJava, source.getConfiguration(), configurationClassName, files);
            generateModel(null, sourcePackage, mainJava, generic ? null : source.getOutputStructure().getStructure(),
                    modelClassName, files);
            return files.stream();
        });
    }

    private void generateModel(final String root, final String packageBase, final String mainJava,
            final ProjectRequest.DataStructure structure, final String modelClassName,
            final Collection<FacetGenerator.InMemoryFile> files) {
        files.add(new FacetGenerator.InMemoryFile(
                mainJava + "/" + packageBase.substring(packageBase.lastIndexOf('.') + 1) + "/" + modelClassName + ".java",
                tpl.render("generator/component/Model.mustache", new HashMap<String, Object>() {

                    {
                        put("className", modelClassName);
                        put("package", packageBase);
                        put("generic", structure == null);
                        if (structure != null && structure.getEntries() != null) {
                            put("structure", structure.getEntries().stream().map(e -> new Property(e.getName(),
                                    capitalize(e.getName()), toJavaConfigType(root, packageBase, e, (fqn, nested) -> {
                                final int li = fqn.lastIndexOf('.');
                                final String pck = li > 0 ? fqn.substring(0, li) : "";
                                final String cn = li > 0 ? fqn.substring(li + 1) : fqn;
                                generateModel((root == null ? "" : root) + capitalize(cn), pck, mainJava,
                                        e.getNestedType(), cn, files);
                            }))).collect(toList()));
                        }
                    }
                })));
    }

    private void generateConfiguration(final String root, final String packageBase, final String mainJava,
            final ProjectRequest.DataStructure structure, final String configurationClassName,
            final Collection<FacetGenerator.InMemoryFile> files) {
        files.add(new FacetGenerator.InMemoryFile(
                mainJava + "/" + packageBase.substring(packageBase.lastIndexOf('.') + 1) + "/" + configurationClassName + ".java",
                tpl.render("generator/component/Configuration.mustache", new HashMap<String, Object>() {

                    {
                        put("className", configurationClassName);
                        put("package", packageBase);
                        put("structure",
                                structure != null && structure.getEntries() != null ?
                                        structure.getEntries().stream().map(e ->
                                                new Property(e.getName(), capitalize(e.getName()),
                                                        toJavaConfigType(root, packageBase, e, (fqn, nested) -> {
                                                            final int li = fqn.lastIndexOf('.');
                                                            final String pck = li > 0 ? fqn.substring(0, li) : "";
                                                            final String cn = li > 0 ? fqn.substring(li + 1) : fqn;
                                                            generateConfiguration((root == null ? "" : root) + capitalize(cn),
                                                                    pck, mainJava, nested, cn, files);
                                                        })))
                                                 .collect(toList()) : emptyList());
                    }
                })));
    }

    @Data
    @AllArgsConstructor
    public static class Property {

        private final String name;

        private final String methodName;

        private final String type;
    }

    @Data
    @AllArgsConstructor
    public static class Connection {

        private final String name;

        private final String javaName;

        private final String type;
    }
}
