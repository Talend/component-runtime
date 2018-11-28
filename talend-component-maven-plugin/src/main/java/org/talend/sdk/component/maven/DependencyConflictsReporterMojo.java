/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.maven;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.maven.api.Audience;

import lombok.Data;

@Audience(TALEND_INTERNAL)
@Mojo(name = "repository-report", defaultPhase = PACKAGE, threadSafe = true,
        requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class DependencyConflictsReporterMojo extends ComponentDependenciesBase {

    @Parameter(property = "talend-dependencies-conflicts.output",
            defaultValue = "${maven.multiModuleProjectDirectory}/target/talend-component-kit/repository-dependency-report.html")
    private File output;

    @Parameter(defaultValue = "Dependency Report", property = "talend-dependencies-conflicts.title")
    private String title;

    @Parameter(property = "talend-dependencies-conflicts.css")
    private String css;

    @Parameter(property = "talend-dependencies-conflicts.js")
    private String js;

    @Override
    public void doExecute() throws MojoExecutionException {
        final MvnDependencyListLocalRepositoryResolver resolver =
                new MvnDependencyListLocalRepositoryResolver("TALEND-INF/dependencies.txt", it -> null);
        final Collection<Item> modules = getArtifacts(artifact -> {
            try (final JarFile file = new JarFile(artifact.getFile())) {
                return ofNullable(file.getEntry("TALEND-INF/dependencies.txt")).map(entry -> {
                    Predicate<org.talend.sdk.component.dependencies.maven.Artifact> ignoredDependencies = it -> false;
                    final JarEntry blacklistEntry = file.getJarEntry("TALEND-INF/dependencies-blacklist.txt");
                    Collection<Artifact> blacklist = emptyList();
                    if (blacklistEntry != null) {
                        try (final InputStream stream = file.getInputStream(blacklistEntry)) {
                            blacklist = resolver
                                    .resolveFromDescriptor(stream)
                                    .sorted(comparing(Artifact::toCoordinate))
                                    .collect(toList());
                            ignoredDependencies = blacklist::contains;
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    try (final InputStream stream = file.getInputStream(entry)) {
                        return new Item(new Artifact(artifact.getGroupId(), artifact.getArtifactId(),
                                artifact.getExtension(), artifact.getClassifier(), artifact.getVersion(), "compile"),
                                blacklist,
                                resolver
                                        .resolveFromDescriptor(stream)
                                        .filter(ignoredDependencies.negate())
                                        .sorted(comparing(Artifact::toCoordinate))
                                        .collect(toList()));
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }).orElse(null);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }).filter(Objects::nonNull).sorted(comparing(a -> a.componentModule.toCoordinate())).collect(toList());

        output.getParentFile().mkdirs();

        final Map<Artifact, ConflictingDependency> conflicts = detectConflicts(modules);

        final Bootstrap bootstrap = new Bootstrap();
        try (final PrintStream stream = new PrintStream(new FileOutputStream(output))) {
            stream.println("<!DOCTYPE html>");
            stream.println("<html lang=\"en\">");
            stream.println(" <head>");
            stream.println("    <meta charset=\"utf-8\">");
            stream
                    .println(
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">");
            stream.println("    <title>" + title + "</title>");
            if (css != null && !css.isEmpty()) {
                stream.println(css);
            } else {
                bootstrap.css(stream);
            }
            stream.println(" </head>");
            stream.println(" <body>");
            stream.println("   <div class=\"container\">");
            stream.println("     <h1>Dependencies Report</h1>");

            if (!conflicts.isEmpty()) {
                stream.println("     <div class=\"alert alert-danger\" role=\"alert\">");
                stream.println("       <strong>Danger!</strong> some dependencies conflict detected!");
                stream.println("     </div>");
            }
            if (modules.stream().flatMap(it -> it.getDependencies().stream()).anyMatch(this::isSnapshot)) {
                stream.println("     <div class=\"alert alert-warning\" role=\"alert\">");
                stream.println("       <strong>Warning!</strong> some dependencies are in snapshot!");
                stream.println("     </div>");
            }

            stream.println("     <ul class=\"nav nav-tabs\" id=\"tabs\" role=\"tablist\">");
            stream.println("       <li class=\"nav-item\">");
            stream
                    .print("         <a class=\"nav-link active\" data-toggle=\"tab\" role=\"tab\" aria-selected=\"true\"");
            stream.println("id=\"conflicts-tab\" href=\"#conflicts\" aria-controls=\"conflicts\">Conflicts</a>");
            stream.println("       </li>");
            stream.println("       <li class=\"nav-item\">");
            stream.print("         <a class=\"nav-link\" data-toggle=\"tab\" role=\"tab\" aria-selected=\"true\"");
            stream.println("id=\"global-tab\" href=\"#global\" aria-controls=\"global\">Global</a>");
            stream.println("       </li>");
            stream.println("       <li class=\"nav-item\">");
            stream.print("         <a class=\"nav-link\" data-toggle=\"tab\" role=\"tab\" aria-selected=\"true\"");
            stream.println("id=\"blacklist-tab\" href=\"#blacklist\" aria-controls=\"blacklist\">Blacklist</a>");
            stream.println("       </li>");
            stream.println("     </ul>");

            stream.println("     <div class=\"tab-content\">");
            stream
                    .println(
                            "       <div class=\"tab-pane active\" id=\"conflicts\" role=\"tabpanel\" aria-labelledby=\"conflicts-tab\">");
            stream.println("         <h2>Conflicting Dependencies Report</h2>");
            writeTable(stream, () -> conflicts.forEach((conflictingDep, meta) -> {
                meta.getModules().stream().limit(1).forEach(module -> {
                    stream.println("             <tr>");
                    stream
                            .println("               <td>" + escapeHtml4(conflictingDep.getGroup() + ':'
                                    + conflictingDep.getArtifact() + ":["
                                    + meta.versions.stream().map(Artifact::getVersion).collect(Collectors.joining("|")))
                                    + "</td>");
                    stream.println("               <td>" + escapeHtml4(module.toCoordinate()) + "</td>");
                    stream.println("             </tr>");
                });
                meta.getModules().stream().skip(1).forEach(module -> {
                    stream.println("             <tr>");
                    stream.println("               <td></td>");
                    stream.println("               <td>" + escapeHtml4(module.toCoordinate()) + "</td>");
                    stream.println("             </tr>");
                });
            }), () -> {
                stream.println("                 <td scope=\"col\">Dependency</td>");
                stream.println("                 <td scope=\"col\">Module</td>");
            });
            stream.println("       </div>");

            stream
                    .println(
                            "       <div class=\"tab-pane\" id=\"global\" role=\"tabpanel\" aria-labelledby=\"global-tab\">");
            stream.println("         <h2>Global Dependencies Report</h2>");
            writeDependencyList(stream, modules, conflicts.keySet(), Item::getDependencies);
            stream.println("       </div>");

            stream
                    .println(
                            "       <div class=\"tab-pane\" id=\"blacklist\" role=\"tabpanel\" aria-labelledby=\"blacklist-tab\">");
            stream.println("         <h2>Repository Dependencies Blacklist</h2>");
            writeDependencyList(stream, modules, emptyList() /* don't highlight anything */, Item::getBlacklist);
            stream.println("       </div>");

            stream.println("     </div>");
            stream.println(" </div>");
            if (js != null && !js.isEmpty()) {
                stream.println(js);
            } else {
                bootstrap.js(stream);
            }
            stream.println(" </body>");
            stream.println("</html>");
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean isSnapshot(final Artifact it) {
        return !it.getGroup().startsWith("org.talend.components") && it.getVersion().contains("-SNAPSHOT");
    }

    private void writeDependencyList(final PrintStream stream, final Collection<Item> modules,
            final Collection<Artifact> conflicts, final Function<Item, Collection<Artifact>> extractor) {
        writeTable(stream, () -> modules.forEach(it -> {
            final Collection<Artifact> deps = extractor.apply(it);
            deps.stream().limit(1).forEach(dep -> {
                stream
                        .println("             <tr" + (conflicts.contains(dep) ? " class=\"table-danger\""
                                : (isSnapshot(dep) ? " class=\"table-warning\"" : "")) + ">");
                stream.println("               <td>" + escapeHtml4(it.componentModule.toCoordinate()) + "</td>");
                stream.println("               <td>" + escapeHtml4(dep.toCoordinate()) + "</td>");
                stream.println("             </tr>");
            });
            deps.stream().skip(1).forEach(dep -> {
                stream.println("             <tr" + (conflicts.contains(dep) ? " class=\"table-danger\"" : "") + ">");
                stream.println("               <td></td>");
                stream.println("               <td>" + escapeHtml4(dep.toCoordinate()) + "</td>");
                stream.println("             </tr>");
            });
        }), () -> {
            stream.println("                 <td scope=\"col\">Module</td>");
            stream.println("                 <td scope=\"col\">Dependency</td>");
        });
    }

    private void writeTable(final PrintStream stream, final Runnable content, final Runnable columns) {
        stream.println("         <div class=\"col-sm-12\">");
        stream.println("           <table class=\"table table-bordered table-striped\">");
        stream.println("             <thead class=\"thead-dark\">");
        stream.println("               <tr>");
        columns.run();
        stream.println("               </tr>");
        stream.println("             </thead>");
        stream.println("             <tbody>");
        content.run();
        stream.println("             </tbody>");
        stream.println("           </table>");
        stream.println("         </div>");
    }

    private Map<Artifact, ConflictingDependency> detectConflicts(final Collection<Item> modules) {
        return modules
                .stream()
                .flatMap(it -> it
                        .getDependencies()
                        .stream()
                        .map(dep -> new AbstractMap.SimpleEntry<>(dep, singletonList(it.getComponentModule()))))
                .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue,
                        (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(toList())))
                .entrySet()
                .stream() // for all deps
                .filter(it -> it.getValue().size() > 1) // used in multiple modules
                .collect(toMap(Map.Entry::getKey,
                        it -> new ConflictingDependency(singleton(it.getKey()), it.getValue())))
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (d1, d2) -> new ConflictingDependency(
                                Stream.concat(d1.versions.stream(), d2.versions.stream()).distinct().collect(toList()),
                                Stream.concat(d1.modules.stream(), d2.modules.stream()).distinct().collect(toList()))))
                .entrySet()
                .stream()
                .filter(it -> it.getValue().getVersions().size() > 1)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Data
    private static class Item {

        private final org.talend.sdk.component.dependencies.maven.Artifact componentModule;

        private final Collection<org.talend.sdk.component.dependencies.maven.Artifact> blacklist;

        private final List<org.talend.sdk.component.dependencies.maven.Artifact> dependencies;
    }

    @Data
    private static class ConflictingDependency {

        private final Collection<Artifact> versions;

        private final Collection<Artifact> modules;
    }
}
