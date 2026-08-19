/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.facet.beam;

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
public class BeamFacet implements FacetGenerator {

    @Inject
    private TemplateRenderer tpl;

    @Inject
    private NameConventions names;

    public void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
    }

    @Override
    public String loggingScope() {
        return "test";
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

    private Stream<InMemoryFile> createSourceTest(final String testJava, final String packageBase,
            final Collection<ProjectRequest.SourceConfiguration> sources) {

        return sources.stream().flatMap(source -> {
            final String baseName = names.toMapperName(source.getName());
            final String testClassName = baseName + "BeamTest";

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
                            tpl.render("generator/facet/beam/BeamSourceTest.mustache", new HashMap<String, Object>() {

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
            final String testClassName = baseName + "BeamTest";
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
                    .add(new FacetGenerator.InMemoryFile(testJava + "/" + classDir + "/" + testClassName + ".java", tpl
                            .render("generator/facet/beam/BeamProcessorTest.mustache", new HashMap<String, Object>() {

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
    public Stream<Dependency> dependencies(final Collection<String> facets, final ServerInfo.Snapshot versions) {
        return Stream
                .of(Dependency.junit(),
                        new Dependency("org.talend.sdk.component", "component-runtime-beam", versions.getKit(), "test"),
                        new Dependency("org.talend.sdk.component", "component-runtime-junit", versions.getKit(),
                                "test"),
                        new Dependency("org.talend.sdk.component", "component-runtime-beam-junit", versions.getKit(),
                                "test"),
                        new Dependency("org.hamcrest", "hamcrest-all", "1.3", "test"),
                        new Dependency("org.apache.beam", "beam-runners-direct-java", versions.getBeam(), "test"),
                        // for avro
                        new Dependency("org.codehaus.jackson", "jackson-core-asl", versions.getAvroJackson(), "test"),
                        new Dependency("org.codehaus.jackson", "jackson-mapper-asl", versions.getAvroJackson(),
                                "test"));
    }

    @Override
    public String description() {
        return "Generates some tests using beam runtime instead of Talend Component Kit Testing framework.";
    }

    @Override
    public String readme() {
        return "Beam facet generates component tests using Apache Beam testing framework. It executes the tests using a real "
                + "beam pipeline to go through the runtime constraints of Apache Beam which can be more strict than Talend Component Kit ones.";
    }

    @Override
    public String name() {
        return "Apache Beam";
    }

    @Override
    public Category category() {
        return Category.RUNTIME;
    }
}
