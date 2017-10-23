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
package org.talend.component.starter.server.service.facet.component;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.component.starter.server.service.domain.Build;
import org.talend.component.starter.server.service.domain.ProjectRequest;
import org.talend.component.starter.server.service.facet.FacetGenerator;
import org.talend.component.starter.server.service.template.TemplateRenderer;

import lombok.AllArgsConstructor;
import lombok.Data;

@ApplicationScoped
public class ComponentGenerator implements FacetGenerator {

    @Inject
    private TemplateRenderer tpl;

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors) {
        final String mainJava = build.getMainJavaDirectory() + '/' + packageBase.replace('.', '/');

        final boolean hasService = (sources != null && !sources.isEmpty()) || (processors != null && !processors.isEmpty());
        if (!hasService) {
            return Stream.empty();
        }

        final String serviceName = toJavaName(build.getArtifact()) + "Service";

        final InMemoryFile packageInfo = new InMemoryFile(mainJava + "/package-info.java",
                tpl.render("generator/component/package-info.java", new HashMap<String, Object>() {

                    {
                        put("package", packageBase + ".service");
                        put("family", build.getArtifact());
                        put("category", build.getArtifact());
                    }
                }));

        return Stream.concat(Stream.of(packageInfo, new InMemoryFile(mainJava + "/service/" + serviceName + ".java",
                tpl.render("generator/component/Service.java", new HashMap<String, Object>() {

                    {
                        put("className", serviceName);
                        put("package", packageBase + ".service");
                    }
                }))),
                Stream.concat(sources == null ? Stream.empty() : sources.stream().flatMap(source -> {
                    final String baseName = toJavaName(source.getName());
                    final String className = baseName + "Source";
                    final String mapperName = baseName + "Mapper";
                    final String configurationClassName = className + "Configuration";
                    final String modelClassName = baseName + "Record";
                    return Stream.of(new InMemoryFile(mainJava + "/source/" + className + ".java",
                            tpl.render("generator/component/Mapper.java", new HashMap<String, Object>() {

                                {
                                    put("className", mapperName);
                                    put("package", packageBase + ".source");
                                    put("serviceName", serviceName);
                                    put("servicePackage", packageBase + ".service");
                                    put("configurationName", configurationClassName);
                                    put("sourceName", className);
                                }
                            })),
                            new InMemoryFile(mainJava + "/source/" + className + ".java",
                                tpl.render("generator/component/Source.java", new HashMap<String, Object>() {

                                    {
                                        put("className", className);
                                        put("package", packageBase + ".source");
                                        put("serviceName", serviceName);
                                        put("servicePackage", packageBase + ".service");
                                        put("configurationName", configurationClassName);
                                        put("modelName", modelClassName);
                                    }
                                })),
                            new InMemoryFile(mainJava + "/source/" + configurationClassName + ".java",
                                tpl.render("generator/component/Configuration.java", new HashMap<String, Object>() {

                                    {
                                        put("className", className);
                                        put("package", packageBase + ".source");
                                        // TODO: configurable configuration
                                    }
                                })),
                            new InMemoryFile(mainJava + "/source/" + modelClassName + ".java",
                                tpl.render("generator/component/Model.java", new HashMap<String, Object>() {

                                    {
                                        put("className", className);
                                        put("package", packageBase + ".source");
                                        put("generic", source.isGenericOutput());
                                        if (!source.isGenericOutput() && source.getOutputStructure() != null
                                                && source.getOutputStructure().getEntries() != null) {
                                            put("structure", source.getOutputStructure().getEntries().stream()
                                                .map(e -> new Property(e.getName(), capitalize(e.getName()), toJavaType(e.getType())))
                                                .collect(toList()));
                                        }
                                    }
                                })));
                }), processors == null ? Stream.empty() : processors.stream().flatMap(processor -> {
                    final String baseName = toJavaName(processor.getName());
                    final String className = baseName + "Processor";
                    final String configurationClassName = className + "Configuration";
                    final String modelClassName = baseName + "Record";
                    // todo: inputs/outputs/templates etc

                    return Stream.of(new InMemoryFile(mainJava + "/processor/" + className + ".java",
                                    tpl.render("generator/component/Processor.java", new HashMap<String, Object>() {

                                        {
                                            put("className", className);
                                            put("package", packageBase + ".source");
                                            put("serviceName", serviceName);
                                            put("servicePackage", packageBase + ".service");
                                            put("configurationName", configurationClassName);
                                            put("modelName", modelClassName);
                                        }
                                    })),
                            new InMemoryFile(mainJava + "/processor/" + configurationClassName + ".java",
                                    tpl.render("generator/component/Configuration.java", new HashMap<String, Object>() {

                                        {
                                            put("className", className);
                                            put("package", packageBase + ".source");
                                            // TODO: configurable configuration
                                        }
                                    }))/*,
                            new InMemoryFile(mainJava + "/source/" + modelClassName + ".java",
                                    tpl.render("generator/component/Model.java", new HashMap<String, Object>() {

                                        {
                                            put("className", className);
                                            put("package", packageBase + ".source");
                                            put("generic", processor.isGenericOutput());
                                            if (!processor.isGenericOutput() && processor.getOutputStructure() != null
                                                    && processor.getOutputStructure().getEntries() != null) {
                                                put("structure", processor.getOutputStructure().getEntries().stream()
                                                                       .map(e -> new Property(e.getName(), capitalize(e.getName()), toJavaType(e.getType())))
                                                                       .collect(toList()));
                                            }
                                        }
                                    }))*/);
                })));
    }

    private String toJavaType(final String type) {
        if (type == null) {
            return "String";
        }
        switch (type.toLowerCase(ENGLISH)) {
            case "boolean":
                return "boolean";
            case "double":
                return "double";
            case "integer":
                return "int";
            default:
                return "String";
        }
    }

    private String toJavaName(final String name) {
        return capitalize(name.replace("-", "_").replace(" ", "_"));
    }

    @Override
    public String description() {
        throw new UnsupportedOperationException("Internal generator");
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("Internal generator");
    }

    @Override
    public Category category() {
        throw new UnsupportedOperationException("Internal generator");
    }

    @Data
    @AllArgsConstructor
    public static class Property {
        private final String name;
        private final String methodName;
        private final String type;
    }
}
