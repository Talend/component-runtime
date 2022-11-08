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
package org.talend.sdk.component.maven;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.CompositeArchive;
import org.apache.xbean.finder.archive.FileArchive;
import org.apache.xbean.finder.archive.JarArchive;
import org.apache.ziplock.IO;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.maven.api.Audience;
import org.talend.sdk.component.runtime.manager.reflect.IconFinder;
import org.talend.sdk.component.server.service.IconResolver;

import lombok.Data;

// not yet a "documented" plugin since it does make sense only for huge component repositories/projects
@Audience(TALEND_INTERNAL)
@Mojo(name = "icon-report", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = COMPILE_PLUS_RUNTIME,
        threadSafe = true)
public class IconReporterMojo extends ClasspathMojoBase {

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "false", property = "talend-component.icon.skip")
    private boolean skip;

    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/target/talend-component-kit/icon-report.html",
            property = "talend-component.icon.output")
    private File output;

    @Parameter(defaultValue = "Icons", property = "talend-component.icon.title")
    private String title;

    @Parameter(property = "talend-component.icon.css")
    private String css;

    @Parameter(property = "talend-component.icon.js")
    private String js;

    private volatile String missingIcon;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!skip) {
            if (missingIcon == null) {
                synchronized (this) {
                    if (missingIcon == null) {
                        final ClassLoader fallbackLoader = IconReporterMojo.class.getClassLoader();
                        try (final InputStream stream = fallbackLoader.getResourceAsStream("icon/missing.png")) {
                            missingIcon = toDataUri(new IconResolver.Icon("image/png", IO.readBytes(stream)));
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }

            executeInLoader();
        }

        final AtomicInteger counter = AtomicInteger.class
                .cast(session
                        .getRequest()
                        .getData()
                        .computeIfAbsent(getClass().getName() + ".counter", k -> new AtomicInteger()));
        if (counter.incrementAndGet() != reactorProjects.size()) {
            getLog().debug("Not yet at the end of the build, skipping rendering");
            return;
        }
        if (!skip) {
            getLog().debug("This is an experimental Mojo");
            getReporter().doReport(output, title, css, js, missingIcon);
            getLog().info("Wrote " + output.getAbsolutePath());
        }
    }

    @Override // todo: findIcon != default but no @Icon
    protected void doExecute() {
        final ExecutionClassLoader loader =
                ExecutionClassLoader.class.cast(Thread.currentThread().getContextClassLoader());
        final AnnotationFinder finder = new AnnotationFinder(
                new CompositeArchive(Stream.of(classes).map(c -> new FileArchive(loader, c)).toArray(Archive[]::new)));

        final List<Class<?>> icons = finder.findAnnotatedClasses(Icon.class);
        final List<Package> packages = finder.findAnnotatedPackages(Icon.class);

        // if talend-icon is there - so a meta icon API can be used - then grab the meta icon
        loader
                .getFiles()
                .stream()
                .filter(it -> it.getName().startsWith("talend-icon-") && it.getName().endsWith(".jar"))
                .findFirst()
                .flatMap(icon -> {
                    try {
                        return new AnnotationFinder(new JarArchive(loader, icon.toURI().toURL()))
                                .findAnnotatedClasses(Icon.class)
                                .stream()
                                .findFirst();
                    } catch (final MalformedURLException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .filter(Class::isAnnotation)
                .map(it -> (Class<? extends Annotation>) it)
                .ifPresent(uiIcon -> {
                    icons.addAll(finder.findAnnotatedClasses(uiIcon));
                    packages.addAll(finder.findAnnotatedPackages(uiIcon));
                });

        if (!icons.isEmpty() && !packages.isEmpty()) {
            final IconFinder iconFinder = new IconFinder();
            final List<IconModel> foundIcons = Stream.concat(icons.stream(), packages.stream()).map(elt -> {
                final boolean isCustom = iconFinder.isCustom(iconFinder.extractIcon(elt));
                final String name = iconFinder.findIcon(elt);
                return new IconModel(project.getArtifactId(), name, findIcon(name), isCustom);
            }).collect(toList());
            final GlobalReporter reporter = getReporter();
            synchronized (reporter) {
                reporter.icons.addAll(foundIcons);
            }
        }
    }

    private String findIcon(final String custom) {
        try (final URLClassLoader loader = new URLClassLoader(
                new URL[] { classes.toURI().toURL(), jarLocation(IconResolver.class).toURI().toURL() },
                new ClassLoader() {

                    @Override
                    public InputStream getResourceAsStream(final String name) {
                        return null;
                    }
                })) {
            return new IconResolver() {

                private final List<String> iconPattern = asList("icons/%s.svg", "icons/%s_icon32.png");

                @Override
                protected boolean isSupportsSvg() {
                    return true;
                }

                @Override
                protected Collection<String> getExtensionPreferences() {
                    return iconPattern;
                }
            }.doLoad(loader, custom).map(this::toDataUri).orElse(missingIcon);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toDataUri(final IconResolver.Icon icon) {
        return "data:" + icon.getType() + ";base64," + Base64.getEncoder().encodeToString(icon.getBytes());
    }

    private GlobalReporter getReporter() {
        synchronized (session) {
            return GlobalReporter.class
                    .cast(session
                            .getRequest()
                            .getData()
                            .computeIfAbsent(getClass().getName() + ".reporter", k -> new GlobalReporter()));
        }
    }

    @Data
    private static class IconModel {

        private final String module;

        private final String name;

        private final String base64;

        private final boolean custom;
    }

    private static class GlobalReporter {

        private final Collection<IconModel> icons = new TreeSet<>(comparing(icon -> icon.module + '#' + icon.name));

        void doReport(final File output, final String title, final String css, final String js, final String missing) {
            final long missingIconsCount = icons.stream().filter(it -> it.base64.equals(missing)).count();
            final long customIconsCount = icons.stream().filter(it -> it.custom).count();
            final long validIcons = icons.size() - missingIconsCount - customIconsCount;

            final Bootstrap bootstrap = new Bootstrap();
            output.getParentFile().mkdirs();
            try (final PrintStream stream = new PrintStream(new FileOutputStream(output))) {
                stream.println("<!DOCTYPE html>");
                stream.println("<html lang=\"en\">");
                stream.println(" <head>");
                stream.println("    <meta charset=\"utf-8\">");
                stream
                        .println(
                                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">");
                stream.println("    <title>" + ofNullable(title).orElse("Icons") + "</title>");
                if (css != null && !css.isEmpty()) {
                    stream.println(css);
                } else {
                    bootstrap.css(stream);
                    stream.println("      <style>");
                    stream.println("        img { max-width: 250px; }");
                    stream.println("        image-container { width: 250px; }");
                    stream.println("      </style>");
                }
                stream.println(" </head>");
                stream.println(" <body>");
                stream.println("   <div class=\"container\">");
                stream.println("     <h1>Components Icons</h1>");
                if (icons.isEmpty()) {
                    stream.println("     <div class=\"alert alert-warning\" role=\"alert\">");
                    stream.println("       <strong>Error!</strong> no icon found!");
                    stream.println("     </div>");
                } else {
                    if (missingIconsCount > 0) {
                        stream.println("     <div class=\"alert alert-danger\" role=\"alert\">");
                        stream.println("       <strong>Danger!</strong> missing icons in use!");
                        stream.println("     </div>");
                    }
                    if (customIconsCount > 0) {
                        stream.println("     <div class=\"alert alert-warning\" role=\"alert\">");
                        stream.println("       <strong>Warning!</strong> custom icons in use!");
                        stream.println("       Check out <a href=\"http://talend.surge.sh/icons/\">surge</a>.");
                        stream.println("     </div>");
                    }
                    stream.println("     <div class=\"col-sm-12\">");
                    stream.println("      <canvas class=\"pieChartReport\"></canvas>");
                    stream.println("     </div>");
                    stream.println("     <div class=\"col-sm-12\">");
                    stream.println("       <table class=\"table table-bordered table-striped\">");
                    stream.println("         <thead class=\"thead-dark\">");
                    stream.println("           <tr>");
                    stream.println("             <td scope=\"col\">Module</td>");
                    stream.println("             <td scope=\"col\">Name</td>");
                    stream.println("             <td scope=\"col\" class=\"image-container\">Preview</td>");
                    stream.println("             <td scope=\"col\">Custom</td>");
                    stream.println("           </tr>");
                    stream.println("         </thead>");
                    stream.println("         <tbody>");
                    stream.println("       </div>");
                    icons.forEach(icon -> {
                        stream
                                .println("         <tr" + (icon.base64.equals(missing) ? " class=\"table-danger\""
                                        : (icon.custom ? " class=\"table-warning\"" : "")) + ">");
                        stream.println("           <td>" + escapeHtml4(icon.module) + "</td>");
                        stream.println("           <td>" + escapeHtml4(icon.name) + "</td>");
                        stream
                                .println("           <td class=\"image-container\"><img src=\"" + icon.base64
                                        + "\"></td>");
                        stream.println("           <td>" + icon.custom + "</td>");
                        stream.println("         </tr>");
                    });
                    stream.println("       </tbody>");
                    stream.println("     </table>");
                    stream.println("   </div>");
                    stream.println(" </div>");
                    if (js != null && !js.isEmpty()) {
                        stream.println(js);
                    } else {
                        bootstrap.js(stream);
                        stream
                                .println("   <script "
                                        + "src=\"https://cdnjs.cloudflare.com/ajax/libs/mdbootstrap/4.5.13/js/mdb.min.js\" "
                                        + "integrity=\"sha256-fgXwKP0uZcyaHIUbCNj9VU/7D8/hJRfeFs/+NuVo51w=\" "
                                        + "crossorigin=\"anonymous\"></script>");
                        stream.println("   <script>");
                        stream.println("     (function () {");
                        stream
                                .println(
                                        "       var ctx = document.getElementsByClassName('pieChartReport')[0].getContext('2d');");
                        stream.println("       new Chart(ctx, {");
                        stream.println("         type: 'pie',");
                        stream.println("         data: {");
                        stream.println("           labels: [ \"Missing Icons\", \"Custom Icons\", \"Valid Icons\" ],");
                        stream.println("           datasets: [{");
                        stream
                                .println("             data: [ " + missingIconsCount + ", " + customIconsCount + ", "
                                        + validIcons + " ],");
                        stream.println("               backgroundColor: [ \"#f8d7da\", \"#fff3cd\", \"#b8daff\" ]");
                        stream.println("           }]");
                        stream.println("         }");
                        stream.println("       });");
                        stream.println("     })();");
                        stream.println("   </script>");
                    }
                }
                stream.println(" </body>");
                stream.println("</html>");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
