/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.facet.testing;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.starter.server.service.Strings.capitalize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.facet.util.NameConventions;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

@ApplicationScoped
public class TalendComponentKitTesting implements FacetGenerator {

    @Inject
    private TemplateRenderer tpl;

    @Inject
    private NameConventions names;

    public void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
    }

    @Override
    public Stream<Dependency> dependencies(final Collection<String> facets, final ServerInfo.Snapshot versions) {
        return Stream
                .of(Dependency.junit(), new Dependency("org.talend.sdk.component", "component-runtime-junit",
                        versions.getKit(), "test"));
    }

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors, final ServerInfo.Snapshot versions) {

        final boolean hasComponent =
                (sources != null && !sources.isEmpty()) || (processors != null && !processors.isEmpty());
        if (!hasComponent) {
            return Stream.empty();
        }

        final String testJava = build.getTestJavaDirectory() + '/' + packageBase.replace('.', '/');
        return Stream
                .concat(createSourceTest(testJava, packageBase, sources),
                        createProcessorsTest(testJava, packageBase, processors));
    }

    @Override
    public String loggingScope() {
        return "test";
    }

    private Stream<InMemoryFile> createSourceTest(final String testJava, final String packageBase,
            final Collection<ProjectRequest.SourceConfiguration> sources) {

        return sources.stream().flatMap(source -> {
            final String baseName = names.toMapperName(source.getName());
            final String testClassName = baseName + "Test";

            final String configurationClassName = names.toConfigurationName(baseName);
            final String mapperName = names.toMapperName(baseName);
            final String outputRecordName = names.toMapperRecordName(source);

            // Configuration structure
            final boolean hasConfig =
                    source.getConfiguration() != null && source.getConfiguration().getEntries() != null
                            && !source.getConfiguration().getEntries().isEmpty();
            final Set<String> configFields = hasConfig
                    ? source.getConfiguration().getEntries().stream().map(e -> capitalize(e.getName())).collect(toSet())
                    : emptySet();

            final Collection<InMemoryFile> files = new ArrayList<>();
            files
                    .add(new FacetGenerator.InMemoryFile(testJava + "/source/" + testClassName + ".java",
                            tpl.render("generator/facet/test/SourceTest.mustache", new HashMap<String, Object>() {

                                {
                                    put("rootPackage", packageBase);
                                    put("classPackage", packageBase + ".source");
                                    put("testClassName", testClassName);
                                    put("sourceClassName", baseName);
                                    put("sourceName", source.getName());
                                    put("mapperName", mapperName);
                                    put("hasConfig", hasConfig);
                                    put("configurationClassName", configurationClassName);
                                    put("configFields", configFields);
                                    put("outputRecordName", outputRecordName);
                                    put("isGeneric", source.getOutputStructure().isGeneric());
                                }
                            })));

            return files.stream();
        });
    }

    private Stream<InMemoryFile> createProcessorsTest(final String testJava, final String packageBase,
            final Collection<ProjectRequest.ProcessorConfiguration> processors) {

        return processors.stream().flatMap(processor -> {
            final boolean isOutput =
                    processor.getOutputStructures() == null || processor.getOutputStructures().isEmpty();
            final String baseName = names.toProcessorName(processor);
            final String testClassName = baseName + "Test";
            final String configurationClassName = names.toConfigurationName(baseName);
            final String classDir = isOutput ? "output" : "processor";

            // Configuration structure
            final boolean hasConfig =
                    processor.getConfiguration() != null && processor.getConfiguration().getEntries() != null
                            && !processor.getConfiguration().getEntries().isEmpty();
            final Set<String> configFields =
                    hasConfig
                            ? processor
                                    .getConfiguration()
                                    .getEntries()
                                    .stream()
                                    .map(e -> capitalize(e.getName()))
                                    .collect(toSet())
                            : emptySet();

            // input branches names
            final Set<Map.Entry<String, String>> inputBranches =
                    processor.getInputStructures().entrySet().stream().flatMap(in -> {
                        final String inName = in.getValue().isGeneric() ? "Record"
                                : capitalize(processor.getName())
                                        + capitalize(names.sanitizeConnectionName(in.getKey())) + "Input";
                        Map<String, String> map = new HashMap<String, String>() {

                            {
                                put(in.getKey(), inName);
                            }
                        };

                        return map.entrySet().stream();
                    }).collect(toSet());

            // outputNames
            final Set<Map.Entry<String, String>> outputBranches =
                    !isOutput ? processor.getOutputStructures().entrySet().stream().flatMap(e -> {
                        final String outName = e.getValue().isGeneric() ? "Record"
                                : capitalize(processor.getName()) + capitalize(names.sanitizeConnectionName(e.getKey()))
                                        + "Output";
                        Map<String, String> map = new HashMap<String, String>() {

                            {
                                put(e.getKey(), outName);
                            }
                        };
                        return map.entrySet().stream();
                    }).collect(toSet()) : emptySet();

            final boolean isGeneric = inputBranches.stream().anyMatch(e -> "Record".equals(e.getValue()))
                    || outputBranches.stream().anyMatch(e -> "Record".equals(e.getValue()));

            final Collection<InMemoryFile> files = new ArrayList<>();
            files
                    .add(new FacetGenerator.InMemoryFile(testJava + "/" + classDir + "/" + testClassName + ".java",
                            tpl.render("generator/facet/test/ProcessorTest.mustache", new HashMap<String, Object>() {

                                {
                                    put("rootPackage", packageBase);
                                    put("classPackage", packageBase + "." + classDir);
                                    put("testClassName", testClassName);
                                    put("processorClassName", baseName);
                                    put("hasConfig", hasConfig);
                                    put("configurationClassName", configurationClassName);
                                    put("configFields", configFields);
                                    put("isOutput", isOutput);
                                    put("processorName", processor.getName());
                                    put("inputBranches", inputBranches);
                                    put("outputBranches", outputBranches);
                                    put("isGeneric", isGeneric);
                                }
                            })));

            return files.stream();
        });
    }

    @Override
    public String name() {
        return "Talend Component Kit Testing";
    }

    @Override
    public Category category() {
        return Category.TEST;
    }

    @Override
    public String readme() {
        return "Talend Component Kit Testing skeleton generator. For each component selected it generates an associated test suffixed with `Test`.";
    }

    @Override
    public String description() {
        return "Generates test(s) for each component.";
    }
}
