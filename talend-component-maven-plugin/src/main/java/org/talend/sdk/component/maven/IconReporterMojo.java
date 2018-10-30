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

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.talend.sdk.component.api.component.Icon.IconType.CUSTOM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
import org.apache.ziplock.IO;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.server.service.IconResolver;

import lombok.Data;

// not yet a "documented" plugin since it does make sense only for huge component reposities/projects
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
                            missingIcon = toDataUri(IO.readBytes(stream));
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

    @Override
    protected void doExecute() {
        final AnnotationFinder finder = new AnnotationFinder(new CompositeArchive(Stream
                .of(classes)
                .map(c -> new FileArchive(Thread.currentThread().getContextClassLoader(), c))
                .toArray(Archive[]::new)));
        final List<Class<?>> icons = finder.findAnnotatedClasses(Icon.class);
        final List<Package> packages = finder.findAnnotatedPackages(Icon.class);
        if (!icons.isEmpty()) {
            final GlobalReporter reporter = getReporter();
            reporter.icons
                    .addAll(Stream
                            .concat(icons.stream(), packages.stream())
                            .map(type -> type.getAnnotation(Icon.class))
                            .map(icon -> {
                                final boolean isCustom = icon.value() == CUSTOM;
                                final String name = isCustom ? icon.custom() : icon.value().getKey();
                                return new IconModel(project.getArtifactId(), name, findIcon(name), isCustom);
                            })
                            .collect(toList()));
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
            return new IconResolver()
                    .doLoad(loader, custom)
                    .map(IconResolver.Icon::getBytes)
                    .map(this::toDataUri)
                    .orElse(missingIcon);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toDataUri(final byte[] icon) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(icon);
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
            final boolean hasMissingIcons = icons.stream().anyMatch(it -> it.base64.equals(missing));
            final boolean hasCustomIcons = icons.stream().anyMatch(it -> it.custom);

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
                    stream
                            .println("    <link rel=\"stylesheet\" "
                                    + "href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css\" "
                                    + "integrity=\"sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm\" "
                                    + "crossorigin=\"anonymous\">");
                    stream.println("      <style>");
                    stream.println("        img { max-width: 250px; }");
                    stream.println("        image-container { width: 250px; }");
                    stream.println("      </style>");
                }
                stream.println(" </head>");
                stream.println(" <body>");
                stream.println("   <div class=\"container\">");
                stream.println("     <h1>Components Icons</h1>");
                if (hasMissingIcons) {
                    stream.println("     <div class=\"alert alert-danger\" role=\"alert\">");
                    stream.println("       <strong>Danger!</strong> missing icons in use!");
                    stream.println("     </div>");
                }
                if (hasCustomIcons) {
                    stream.println("     <div class=\"alert alert-warning\" role=\"alert\">");
                    stream.println("       <strong>Warning!</strong> custom icons in use!");
                    stream.println("     </div>");
                }
                stream.println("     <table class=\"table table-bordered table-striped\">");
                stream.println("       <thead class=\"thead-dark\">");
                stream.println("         <tr>");
                stream.println("           <td scope=\"col\">Module</td>");
                stream.println("           <td scope=\"col\">Name</td>");
                stream.println("           <td scope=\"col\" class=\"image-container\">Preview</td>");
                stream.println("           <td scope=\"col\">Custom</td>");
                stream.println("         </tr>");
                stream.println("       </thead>");
                stream.println("       <tbody>");
                icons.forEach(icon -> {
                    stream
                            .println("         <tr" + (icon.base64.equals(missing) ? " class=\"table-danger\""
                                    : (icon.custom ? " class=\"table-warning\"" : "")) + ">");
                    stream.println("           <td>" + escapeHtml4(icon.module) + "</td>");
                    stream.println("           <td>" + escapeHtml4(icon.name) + "</td>");
                    stream.println("           <td class=\"image-container\"><img src=\"" + icon.base64 + "\"></td>");
                    stream.println("           <td>" + icon.custom + "</td>");
                    stream.println("         </tr>");
                });
                stream.println("       </tbody>");
                stream.println("     </table>");
                stream.println("   </div>");
                if (js != null && !js.isEmpty()) {
                    stream.println(js);
                } else {
                    stream
                            .println("   <script src=\"https://code.jquery.com/jquery-3.2.1.slim.min.js\" "
                                    + "integrity=\"sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN\" "
                                    + "crossorigin=\"anonymous\"></script>\n");
                    stream
                            .println("<script "
                                    + "src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js\" "
                                    + "integrity=\"sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q\" "
                                    + "crossorigin=\"anonymous\"></script>\n");
                    stream
                            .println(
                                    "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js\" "
                                            + "integrity=\"sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl\" "
                                            + "crossorigin=\"anonymous\"></script>");
                }
                stream.println(" </body>");
                stream.println("</html>");
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
