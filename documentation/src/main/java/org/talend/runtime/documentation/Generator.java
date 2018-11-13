/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.runtime.documentation;

import static java.lang.Math.min;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.FileArchive;
import org.apache.xbean.finder.archive.JarArchive;
import org.apache.ziplock.IO;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.jruby.Ruby;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.condition.meta.Condition;
import org.talend.sdk.component.api.configuration.constraint.meta.Validation;
import org.talend.sdk.component.api.configuration.constraint.meta.Validations;
import org.talend.sdk.component.api.configuration.type.meta.ConfigurationType;
import org.talend.sdk.component.api.configuration.ui.layout.AutoLayout;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.meta.Ui;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.asyncvalidation.ValidationResult;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.Values;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;
import org.talend.sdk.component.api.service.schema.Schema;
import org.talend.sdk.component.api.service.schema.Type;
import org.talend.sdk.component.junit.environment.BaseEnvironmentProvider;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ConditionParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ConfigurationTypeParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.UiParameterEnricher;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.ValidationParameterEnricher;
import org.talend.sdk.component.runtime.manager.xbean.KnownClassesFilter;
import org.talend.sdk.component.runtime.manager.xbean.KnownJarsFilter;
import org.talend.sdk.component.runtime.record.SchemaImpl;
import org.talend.sdk.component.runtime.reflect.Defaults;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Generator {

    public static void main(final String[] args) {
        if (Boolean.parseBoolean(args[7]) || Boolean.getBoolean(System.getenv("TRAVIS"))) {
            log.info("Skipping doc generation as requested");
            return;
        }

        final File generatedDir = new File(args[0], "_partials");
        generatedDir.mkdirs();

        final File iconOutput = new File(generatedDir.getParentFile().getParentFile(), "assets/images/icons");

        try (final Tasks tasks = new Tasks()) {
            tasks.register((ThrowingSupplier<Asciidoctor>) Asciidoctor.Factory::create).thenApply(adoc -> {
                generatedDocumentationIndex(generatedDir, adoc);
                return null;
            }).thenAccept(ignored -> {
                // shutdown jruby which leaks threads by default
                Ruby.getGlobalRuntime().getJITCompiler().tearDown();
            });
            tasks.register(() -> generatedTypes(generatedDir));
            tasks.register(() -> generatedConstraints(generatedDir));
            tasks.register(() -> generatedConditions(generatedDir));
            tasks.register(() -> generatedActions(generatedDir));
            tasks.register(() -> generatedUi(generatedDir));
            tasks.register(() -> generatedServerConfiguration(generatedDir));
            tasks.register(() -> updateComponentServerApi(generatedDir));
            tasks.register(() -> generatedJUnitEnvironment(generatedDir));
            tasks.register(() -> generatedScanningExclusions(generatedDir));
            tasks.register(() -> generatedIcons(generatedDir, iconOutput));

            final boolean offline = "offline=true".equals(args[4]);
            if (offline) {
                log.info("System is offline, skipping jira changelog and github contributor generation");
            } else {
                tasks.register(() -> generatedContributors(generatedDir, args[5], args[6]));
                tasks.register(() -> generatedJira(generatedDir, args[1], args[2], args[3]));
            }
        }
    }

    private static void updateComponentServerApi(final File generatedDir) throws Exception {
        final File output = new File(generatedDir, "generated_rest-resources.adoc");
        try (final InputStream source = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("META-INF/resources/documentation/openapi.json");
                final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig())) {
            final String newJson = IO.slurp(source);
            String oldJson = !output.exists() ? "{}" : String.join("\n", Files.readAllLines(output.toPath()));
            final int start = oldJson.indexOf(".swaggerUi = ");
            if (start > 0) {
                oldJson = oldJson.substring(start + ".swaggerUi = ".length());
            }

            final int end = oldJson.indexOf(";</script>");
            if (end > 0) {
                oldJson = oldJson.substring(0, end);
            }
            final JsonBuilderFactory builderFactory = Json.createBuilderFactory(emptyMap());
            final JsonObject oldApi = !oldJson.startsWith("{") ? builderFactory.createObjectBuilder().build()
                    : jsonb.fromJson(oldJson, JsonObject.class);
            final JsonObject newApi = builderFactory
                    .createObjectBuilder(jsonb.fromJson(newJson, JsonObject.class))
                    .add("servers",
                            builderFactory
                                    .createArrayBuilder()
                                    .add(builderFactory
                                            .createObjectBuilder()
                                            .add("url", "https://tacokitexample.000webhostapp.com")))
                    .build();
            if (!oldJson.startsWith("{") || !areEqualsIgnoringOrder(oldApi, newApi)) {
                try (final OutputStream writer = new WriteIfDifferentStream(output)) {
                    writer
                            .write(("= Component Server API\n:page-talend_swaggerui:\n\n++++\n<script>\n"
                                    + "(window.talend = (window.talend || {})).swaggerUi = " + newApi.toString()
                                    + ";</script>\n" + "<div id=\"swagger-ui\"></div>\n++++\n")
                                            .getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static boolean areEqualsIgnoringOrder(final JsonValue oldValue, final JsonValue newValue) {
        if (!oldValue.getValueType().equals(newValue.getValueType())) {
            return false;
        }
        switch (oldValue.getValueType()) {
        case STRING:
            return JsonString.class.cast(oldValue).getString().equals(JsonString.class.cast(newValue).getString());
        case NUMBER:
            return JsonNumber.class.cast(oldValue).doubleValue() == JsonNumber.class.cast(newValue).doubleValue();
        case OBJECT:
            final JsonObject oldObject = oldValue.asJsonObject();
            final JsonObject newObject = newValue.asJsonObject();
            if (!oldObject.keySet().equals(newObject.keySet())) {
                return false;
            }
            return oldObject
                    .keySet()
                    .stream()
                    .map(key -> areEqualsIgnoringOrder(oldObject.get(key), newObject.get(key)))
                    .reduce(true, (a, b) -> a && b);
        case ARRAY:
            final JsonArray oldArray = oldValue.asJsonArray();
            final JsonArray newArray = newValue.asJsonArray();
            if (oldArray.size() != newArray.size()) {
                return false;
            }
            if (oldArray.isEmpty()) {
                return true;
            }
            for (final JsonValue oldItem : oldArray) {
                if (newArray.stream().noneMatch(newitem -> areEqualsIgnoringOrder(oldItem, newitem))) {
                    return false;
                }
            }
            return true;
        default:
            // value type check was enough
            return true;
        }
    }

    private static void generatedDocumentationIndex(final File generatedDir, final Asciidoctor asciidoctor) {
        final File baseDir = jarLocation(Generator.class).getParentFile().getParentFile();
        final File pages = new File(baseDir, "src/main/antora/modules/ROOT/pages/");

        final Collection<DocumentationItem> items = Stream
                .of(requireNonNull(pages.listFiles(), "Missing pages"))
                .filter(f -> f.getName().endsWith(".adoc"))
                .map(file -> {
                    final Document document = asciidoctor.loadFile(file, emptyMap());
                    if (document
                            .getAttributes()
                            .keySet()
                            .stream()
                            .noneMatch(it -> it.startsWith("page-documentationindex"))) {
                        return null;
                    }
                    final String name = file.getName();
                    return new DocumentationItem(
                            ofNullable(document.getAttribute("page-documentationindex-index"))
                                    .map(String::valueOf)
                                    .map(Integer::parseInt)
                                    .orElse(Integer.MAX_VALUE),
                            ofNullable(document.getAttribute("page-documentationindex-icon"))
                                    .map(String::valueOf)
                                    .orElse("link"),
                            ofNullable(document.getAttribute("page-documentationindex-label"))
                                    .map(String::valueOf)
                                    .orElseGet(document::getTitle),
                            ofNullable(document.getAttribute("page-documentationindex-description"))
                                    .map(String::valueOf)
                                    .orElseGet(() -> ofNullable(document.getDoctitle()).orElse(document.getTitle())),
                            name.substring(0, name.length() - ".adoc".length()) + ".html");
                })
                .filter(Objects::nonNull)
                .sorted(comparing(DocumentationItem::getIndex))
                .collect(toList());

        final File file = new File(generatedDir, "generated_documentationindex.adoc");
        try (final Jsonb jsonb = newJsonb(); final OutputStream writer = new WriteIfDifferentStream(file)) {
            writer.write("++++\n<jsonArray>".getBytes(StandardCharsets.UTF_8));
            writer.write(jsonb.toJson(items).getBytes(StandardCharsets.UTF_8));
            writer.write("</jsonArray>\n++++".getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void processSection(final StructuralNode node, final StringBuilder builder, final String target) {
        node.getBlocks().stream().filter(Section.class::isInstance).map(Section.class::cast).forEach(section -> {
            final boolean root = section.getLevel() == 1;
            builder
                    .append(root ? "."
                            : (IntStream.range(0, section.getLevel() - 1).mapToObj(i -> "*").collect(joining())
                                    + " xref:" + target + "#" + section.getId() + "["))
                    .append(root ? mapTitle(section.getTitle()) : section.getTitle())
                    .append(root ? "" : "]")
                    .append('\n');
            processSection(section, builder, target);
            if (root) {
                builder.append('\n');
            }
        });
    }

    private static String mapTitle(final String title) {
        switch (title) {
        case "Talend Components Definitions Documentation":
            return "Programming Model";
        case "Components Packaging":
            return "Package";
        case ".Build tools":
            return "Build";
        case "Talend Component Testing Documentation":
            return "Testing";
        default:
            return title;
        }
    }

    private static void generatedScanningExclusions(final File generatedDir) {
        final File file = new File(generatedDir, "generated_scanning-exclusions.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println("= Jars Scanning");
            stream.println();
            stream.println("To find components the framework can scan the classpath but in this case,");
            stream.println("to avoid to scan the whole classpath which can be really huge");
            stream.println("an impacts a lot the startup time, several jars are excluded out of the box.");
            stream.println();
            stream.println("These jars use the following prefix:");
            stream.println();
            stream.println("[.talend-filterlist]");
            new KnownJarsFilter()
                    .getExcludes()
                    .stream()
                    .sorted()
                    .distinct()
                    .map(prefix -> "- " + prefix)
                    .forEach(stream::println);
            stream.println();
            stream.println();
            stream.println("= Package Scanning");
            stream.println();
            stream.println("Since the framework can be used in the case of __fatjars__ or __shades__,");
            stream.println("and because it still uses scanning,");
            stream.println("it is important to ensure we don't scan the whole classes for performances reason.");
            stream.println();
            stream.println("Therefore, the following packages are ignored:");
            stream.println();
            stream.println("[.talend-filterlist]");
            KnownClassesFilter.OptimizedExclusionFilter.class
                    .cast(KnownClassesFilter.class.cast(KnownClassesFilter.INSTANCE).getDelegateSkip())
                    .getIncluded()
                    .stream()
                    .sorted()
                    .distinct()
                    .map(prefix -> "- " + prefix)
                    .forEach(stream::println);
            stream.println();
            stream.println();
            stream.println("NOTE: it is not recommanded but possible to add in your plugin module a");
            stream.println("`TALEND-INF/scanning.properties` file with `classloader.includes` and");
            stream.println("`classloader.excludes` entries to refine the scanning with custom rules.");
            stream.println("In such a case, exclusions win over inclusions.");
            stream.println();
            stream.println();
        }
    }

    private static void generatedJUnitEnvironment(final File generatedDir) throws MalformedURLException {
        final File file = new File(generatedDir, "generated_junit-environments.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println();
            stream.println("NOTE: the configuration is read from system properties, environment variables, ....");
            stream.println();
            final File api = jarLocation(BaseEnvironmentProvider.class);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final AnnotationFinder finder = new AnnotationFinder(
                    api.isDirectory() ? new FileArchive(loader, api) : new JarArchive(loader, api.toURI().toURL()));
            finder
                    .link()
                    .findSubclasses(BaseEnvironmentProvider.class)
                    .stream()
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .sorted(Comparator.comparing(Class::getName))
                    .forEach(type -> {
                        final BaseEnvironmentProvider environment;
                        try {
                            environment = BaseEnvironmentProvider.class.cast(type.getConstructor().newInstance());
                        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException
                                | InvocationTargetException e) {
                            throw new IllegalStateException(e);
                        }
                        stream.println(environment.getName() + ":: " + "__class: " + type.getSimpleName() + "_. ");
                    });
            stream.println();
        }
    }

    private static void generatedContributors(final File generatedDir, final String user, final String pwd)
            throws Exception {
        final Collection<Contributor> contributors;
        if (user == null || user.trim().isEmpty() || "skip".equals(user)) {
            log.error("No Github credentials, will skip contributors generation");
            return;
        } else {
            try {
                contributors = new Github(user, pwd).load();
            } catch (final RuntimeException re) {
                log.error(re.getMessage(), re);
                return;
            }
        }
        final File file = new File(generatedDir, "generated_contributors.adoc");
        try (final Jsonb jsonb = newJsonb(); final OutputStream writer = new WriteIfDifferentStream(file)) {
            writer.write("++++\n<jsonArray>".getBytes(StandardCharsets.UTF_8));
            writer.write(jsonb.toJson(contributors).getBytes(StandardCharsets.UTF_8));
            writer.write("</jsonArray>\n++++".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void generatedIcons(final File generatedDir, final File source) throws Exception {
        if (!source.exists()) {
            log.warn("{} does not exist", source);
            return;
        }
        final List<Icon> icons = new ArrayList<>();
        final File file = new File(generatedDir, "generated_icons.adoc");
        final Path iconBase = source.toPath();
        Files.walkFileTree(iconBase, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".svg")) {
                    icons
                            .add(new Icon(
                                    file
                                            .getFileName()
                                            .toString()
                                            .replace(".svg", "")
                                            .replace('-', '_')
                                            .toUpperCase(ENGLISH),
                                    "_images/icons/" + iconBase.relativize(file).toString(),
                                    file.toFile().getParentFile().getName().equals("svg-deprecated")));
                }
                return super.visitFile(file, attrs);
            }
        });
        if (icons.isEmpty()) {
            log.error("Can't collect icons in {}", source);
            return;
        }
        icons.sort(comparing(Icon::getName));
        try (final Jsonb jsonb = newJsonb(); final OutputStream writer = new WriteIfDifferentStream(file)) {
            writer.write("++++\n<jsonArray>".getBytes(StandardCharsets.UTF_8));
            writer.write(jsonb.toJson(icons).getBytes(StandardCharsets.UTF_8));
            writer.write("</jsonArray>\n++++".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void generatedJira(final File generatedDir, final String username, final String password,
            final String version) {
        if (username == null || username.trim().isEmpty() || "skip".equals(username)) {
            log.error("No JIRA credentials, will skip changelog generation");
            return;
        }

        final String project = "TCOMP";
        final String jiraBase = "https://jira.talendforge.org";

        final File file = new File(generatedDir, "generated_changelog.adoc");
        final Client client = ClientBuilder.newClient().register(new JsonbJaxrsProvider<>());
        final String auth = "Basic "
                + Base64.getEncoder().encodeToString((username + ':' + password).getBytes(StandardCharsets.UTF_8));

        try {
            final WebTarget restApi =
                    client.target(jiraBase + "/rest/api/2").property("http.connection.timeout", 60000L);
            final List<JiraVersion> versions = restApi
                    .path("project/{project}/versions")
                    .resolveTemplate("project", project)
                    .request(APPLICATION_JSON_TYPE)
                    .header("Authorization", auth)
                    .get(new GenericType<List<JiraVersion>>() {
                    });

            final String currentVersion = version.replace("-SNAPSHOT", "");
            final List<JiraVersion> loggedVersions = versions
                    .stream()
                    .filter(v -> (v.isReleased() || jiraVersionMatches(currentVersion, v.getName())))
                    .collect(toList());
            if (loggedVersions.isEmpty()) {
                try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
                    stream.println("No version found.");
                }
                return;
            }

            final int maxVersionPerQuery = 10;
            final BiFunction<String, Long, JiraIssues> searchFrom = (jql, startAt) -> restApi
                    .path("search")
                    .queryParam("jql", jql)
                    .queryParam("startAt", startAt)
                    .request(APPLICATION_JSON_TYPE)
                    .header("Authorization", auth)
                    .get(JiraIssues.class);
            final BiFunction<String, JiraIssues, Stream<JiraIssues>> paginate =
                    new BiFunction<String, JiraIssues, Stream<JiraIssues>>() {

                        @Override
                        public Stream<JiraIssues> apply(final String jql, final JiraIssues issues) {
                            final long nextStartAt = issues.getStartAt() + issues.getMaxResults();
                            final Stream<JiraIssues> fetched = Stream.of(issues);
                            return issues.getTotal() > nextStartAt
                                    ? Stream.concat(fetched, apply(jql, searchFrom.apply(jql, nextStartAt))).parallel()
                                    : fetched;
                        }
                    };
            final Set<String> includeStatus =
                    Stream.of("closed", "resolved", "development done", "qa done", "done").collect(toSet());

            final Map<String, TreeMap<String, List<JiraIssue>>> issues = IntStream
                    .range(0, (loggedVersions.size() + maxVersionPerQuery - 1) / maxVersionPerQuery)
                    .mapToObj(pageIdx -> loggedVersions
                            .subList(pageIdx * maxVersionPerQuery,
                                    min(maxVersionPerQuery * (pageIdx + 1), loggedVersions.size())))
                    .map(pageVersions -> "project=" + project + " AND labels=\"changelog\""
                            + pageVersions
                                    .stream()
                                    .map(v -> "fixVersion=" + v.getName())
                                    .collect(joining(" OR ", " AND (", ")")))
                    .flatMap(jql -> Stream
                            .of(searchFrom.apply(jql, 0L))
                            .flatMap(it -> paginate.apply(jql, it))
                            .flatMap(i -> ofNullable(i.getIssues()).map(Collection::stream).orElseGet(Stream::empty))
                            .filter(issue -> includeStatus
                                    .contains(issue.getFields().getStatus().getName().toLowerCase(ENGLISH)))
                            .flatMap(i -> i.getFields().getFixVersions().stream().map(v -> Pair.of(v, i))))
                    .collect(groupingBy(pair -> pair.getKey().getName(), () -> new TreeMap<>(versionComparator()),
                            groupingBy(pair -> pair.getValue().getFields().getIssuetype().getName(), TreeMap::new,
                                    collectingAndThen(mapping(Pair::getValue, toList()), l -> {
                                        l.sort(comparing(JiraIssue::getKey));
                                        return l;
                                    }))));

            final String changelog = issues
                    .entrySet()
                    .stream()
                    .map(versionnedIssues -> new StringBuilder("\n\n== Version ")
                            .append(versionnedIssues.getKey())
                            .append(versionnedIssues
                                    .getValue()
                                    .entrySet()
                                    .stream()
                                    .collect((Supplier<StringBuilder>) StringBuilder::new,
                                            (builder, issuesByType) -> builder
                                                    .append("\n\n=== ")
                                                    .append(issuesByType.getKey())
                                                    .append("\n\n")
                                                    .append(issuesByType
                                                            .getValue()
                                                            .stream()
                                                            .collect((Supplier<StringBuilder>) StringBuilder::new,
                                                                    // note: for now we don't
                                                                    // use the description since
                                                                    // it is not that
                                                                    // useful
                                                                    (a, i) -> a
                                                                            .append("- link:")
                                                                            .append(jiraBase)
                                                                            .append("/browse/")
                                                                            .append(i.getKey())
                                                                            .append("[")
                                                                            .append(i.getKey())
                                                                            .append("^]")
                                                                            .append(": ")
                                                                            .append(i.getFields().getSummary().trim())
                                                                            .append("\n"),
                                                                    StringBuilder::append)),
                                            StringBuilder::append)))
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();

            try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
                stream.println(changelog);
            }
        } finally {
            client.close();
        }
    }

    private static Comparator<String> versionComparator() {
        return (o1, o2) -> {
            final String[] parts1 = o1.split("\\.");
            final String[] parts2 = o2.split("\\.");
            for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
                try {
                    final int major = (parts2.length > i ? Integer.parseInt(parts2[i]) : 0)
                            - (parts1.length > i ? Integer.parseInt(parts1[i]) : 0);
                    if (major != 0) {
                        return major;
                    }
                } catch (final NumberFormatException nfe) {
                    // no-op
                }
            }
            return o2.compareTo(o1);
        };
    }

    private static boolean jiraVersionMatches(final String ref, final String name) {
        return ref.equals(name) || ref.equals(name + ".0");
    }

    private static void generatedServerConfiguration(final File generatedDir) {
        final File file = new File(generatedDir, "generated_server-configuration.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println();
            stream.println("NOTE: the configuration is read from system properties, environment variables, ....");
            stream.println();
            generateConfigTableContent(stream, ComponentServerConfiguration.class);
            stream.println();
        }
    }

    private static void generateConfigTableContent(final PrintStream stream, final Class<?>... configClass) {
        Stream
                .of(configClass)
                .flatMap(c -> Stream.of(c.getDeclaredFields()))
                .filter(field -> field.isAnnotationPresent(org.eclipse.microprofile.config.inject.ConfigProperty.class))
                .map(field -> {
                    final org.eclipse.microprofile.config.inject.ConfigProperty configProperty =
                            field.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class);
                    final String name =
                            field.getAnnotation(org.eclipse.microprofile.config.inject.ConfigProperty.class).name();
                    return name + ":: " + of(configProperty.defaultValue())
                            .filter(it -> !it
                                    .equals(org.eclipse.microprofile.config.inject.ConfigProperty.UNCONFIGURED_VALUE))
                            .map(it -> "Default value: `" + it + "`. ")
                            .orElse("")
                            + Stream
                                    .of(field.getDeclaredAnnotations())
                                    .filter(a -> a.annotationType().getSimpleName().equals("Documentation"))
                                    .map(a -> {
                                        try {
                                            return a.annotationType().getMethod("value").invoke(a).toString();
                                        } catch (final IllegalAccessException | InvocationTargetException
                                                | NoSuchMethodException e) {
                                            return "-";
                                        }
                                    })
                                    .findFirst()
                                    .orElse("-");
                })
                .sorted()
                .forEach(stream::println);
    }

    private static void generatedActions(final File generatedDir) throws Exception {
        final File file = new File(generatedDir, "generated_actions.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println();
            final File api = jarLocation(ActionType.class);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final AnnotationFinder finder = new AnnotationFinder(
                    api.isDirectory() ? new FileArchive(loader, api) : new JarArchive(loader, api.toURI().toURL()));
            try (final Jsonb jsonb = newJsonb()) {
                finder
                        .findAnnotatedClasses(ActionType.class)
                        .stream()
                        .sorted(Comparator
                                .comparing(t -> t.getAnnotation(ActionType.class).value() + "#" + t.getSimpleName()))
                        .forEach(type -> {
                            final ActionType actionType = type.getAnnotation(ActionType.class);
                            final Class<?> returnedType = actionType.expectedReturnedType();
                            final String actionTypeValue = actionType.value();
                            stream.println();
                            stream.println("= " + capitalizeWords(actionTypeValue));
                            stream.println();
                            stream.println(extractDoc(type));
                            stream.println();
                            stream.println("- Type: `" + actionTypeValue + "`");
                            stream.println("- API: `@" + type.getName() + "`");
                            if (returnedType != Object.class) {
                                stream.println("- Returned type: `" + returnedType.getName() + "`");
                                stream.println("- Sample:");
                                stream.println();
                                stream.println("[source,js]");
                                stream.println("----");
                                stream.println(sample(jsonb, returnedType));
                                stream.println("----");
                            }
                            stream.println();
                        });
            }
            stream.println();

        }
    }

    private static String capitalizeWords(final String actionTypeValue) {
        return Stream.of(actionTypeValue.replace("_", " ").split(" ")).map(WordUtils::capitalize).collect(joining(" "));
    }

    private static void generatedUi(final File generatedDir) throws Exception {
        final File file = new File(generatedDir, "generated_ui.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println();
            final File api = jarLocation(Ui.class);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final AnnotationFinder finder = new AnnotationFinder(
                    api.isDirectory() ? new FileArchive(loader, api) : new JarArchive(loader, api.toURI().toURL()));
            final ParameterExtensionEnricher enricher = new UiParameterEnricher();
            try (final Jsonb jsonb = newJsonb()) {
                finder
                        .findAnnotatedClasses(Ui.class)
                        .stream()
                        .sorted(Comparator.comparing(Class::getName))
                        .forEach(type -> {
                            final Map<String, String> meta = new TreeMap<>(enricher
                                    .onParameterAnnotation("theparameter", Object.class, generateAnnotation(type))
                                    .entrySet()
                                    .stream()
                                    .collect(toMap(e -> e.getKey().replace("tcomp::", ""), Map.Entry::getValue)));
                            stream.println();
                            stream.println("= @" + type.getSimpleName());
                            stream.println();
                            stream.println(extractDoc(type));
                            stream.println();
                            stream.println("- API: `@" + type.getName() + "`");
                            stream.println();
                            stream.println("Sample:");
                            stream.println();
                            stream.println("[source,js]");
                            stream.println("----");
                            stream.println(jsonb.toJson(meta));
                            stream.println("----");
                            stream.println();
                        });
            }
            stream.println();

        }
    }

    private static String sample(final Jsonb jsonb, final Class<?> returnedType) {
        if (returnedType == Values.class) {
            final Values list = new Values();
            list.setItems(new ArrayList<>());

            final Values.Item item = new Values.Item();
            item.setId("value");
            item.setLabel("label");
            list.getItems().add(item);

            return jsonb.toJson(list);
        }
        if (returnedType == SuggestionValues.class) {
            final SuggestionValues list = new SuggestionValues();
            list.setItems(new ArrayList<>());

            final SuggestionValues.Item item = new SuggestionValues.Item();
            item.setId("value");
            item.setLabel("label");
            list.getItems().add(item);

            return jsonb.toJson(list);
        }
        if (returnedType == HealthCheckStatus.class) {
            final HealthCheckStatus status = new HealthCheckStatus();
            status.setStatus(HealthCheckStatus.Status.KO);
            status.setComment("Something went wrong");
            return jsonb.toJson(status);
        }
        if (returnedType == Schema.class) {
            final Schema.Entry entry = new Schema.Entry();
            entry.setName("column1");
            entry.setType(Type.STRING);

            final Schema schema = new Schema();
            schema.setEntries(new ArrayList<>());
            schema.getEntries().add(entry);
            return jsonb.toJson(schema);
        }
        if (returnedType == org.talend.sdk.component.api.record.Schema.class) {
            return jsonb
                    .toJson(new SchemaImpl.BuilderImpl()
                            .withType(org.talend.sdk.component.api.record.Schema.Type.RECORD)
                            .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                                    .withName("column1")
                                    .withType(org.talend.sdk.component.api.record.Schema.Type.STRING)
                                    .withNullable(false)
                                    .withComment("The column 1")
                                    .build())
                            .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                                    .withName("column2")
                                    .withType(org.talend.sdk.component.api.record.Schema.Type.INT)
                                    .withComment("The int column")
                                    .build())
                            .build());
        }
        if (returnedType == ValidationResult.class) {
            final ValidationResult status = new ValidationResult();
            status.setStatus(ValidationResult.Status.KO);
            status.setComment("Something went wrong");
            return jsonb.toJson(status);
        }
        return "{\n" + Stream
                .of(returnedType.getDeclaredFields())
                .map(f -> " \"" + f.getName() + "\": " + createSample(f.getType()))
                .collect(joining("\n")) + "\n}";
    }

    private static String createSample(final Class<?> type) {
        if (type.isEnum()) {
            return Stream
                    .of(type.getEnumConstants())
                    .map(e -> Enum.class.cast(e).name())
                    .collect(joining("\"|\"", "\"", "\""));
        }
        return "\"...\"";
    }

    private static void generatedConditions(final File generatedDir) throws Exception {
        final File file = new File(generatedDir, "generated_conditions.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println();
            final File api = jarLocation(Condition.class);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final ConditionParameterEnricher enricher = new ConditionParameterEnricher();
            try (final Jsonb jsonb = newJsonb()) {
                final AnnotationFinder finder = new AnnotationFinder(
                        api.isDirectory() ? new FileArchive(loader, api) : new JarArchive(loader, api.toURI().toURL()));
                finder
                        .findAnnotatedClasses(Condition.class)
                        .stream()
                        .sorted(comparing(Class::getName))
                        .forEach(type -> {
                            stream.println();
                            stream.println("= " + capitalizeWords(type.getSimpleName()));
                            stream.println();
                            stream.println(extractDoc(type));
                            stream.println();
                            stream.println("- API: `@" + type.getName() + "`");
                            stream.println("- Type: `" + type.getAnnotation(Condition.class).value() + "`");
                            stream.println("- Sample:");
                            stream.println();
                            stream.println("[source,js]");
                            stream.println("----");
                            stream
                                    .println(jsonb
                                            .toJson(new TreeMap<>(enricher
                                                    .onParameterAnnotation("test", String.class,
                                                            generateAnnotation(type))))
                                            .replace("tcomp::", ""));
                            stream.println("----");
                            stream.println();
                        });
                stream.println();
            }

        }
    }

    private static void generatedTypes(final File generatedDir) throws Exception {
        final File file = new File(generatedDir, "generated_configuration-types.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            final File api = jarLocation(ConfigurationType.class);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final ConfigurationTypeParameterEnricher enricher = new ConfigurationTypeParameterEnricher();
            final AnnotationFinder finder = new AnnotationFinder(
                    api.isDirectory() ? new FileArchive(loader, api) : new JarArchive(loader, api.toURI().toURL()));
            try (final Jsonb jsonb = newJsonb()) {
                finder
                        .findAnnotatedClasses(ConfigurationType.class)
                        .stream()
                        .sorted(comparing(Class::getName))
                        .forEach(type -> {
                            stream.println();
                            stream.println("= " + capitalizeWords(type.getAnnotation(ConfigurationType.class).value()));
                            stream.println();
                            stream.println(extractDoc(type));
                            stream.println();
                            stream.println("- API: @" + type.getName());
                            stream.println("- Sample:");
                            stream.println();
                            stream.println("[source,js]");
                            stream.println("----");
                            stream
                                    .println(jsonb
                                            .toJson(new TreeMap<>(enricher
                                                    .onParameterAnnotation("value", String.class,
                                                            generateAnnotation(type)))));
                            stream.println("----");
                            stream.println();
                        });
            }
            stream.println();

        }
    }

    private static void generatedConstraints(final File generatedDir) throws Exception {
        final File file = new File(generatedDir, "generated_constraints.adoc");
        try (final PrintStream stream = new PrintStream(new WriteIfDifferentStream(file))) {
            stream.println();
            final File api = jarLocation(Validation.class);
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            final AnnotationFinder finder = new AnnotationFinder(
                    api.isDirectory() ? new FileArchive(loader, api) : new JarArchive(loader, api.toURI().toURL()));
            final ValidationParameterEnricher enricher = new ValidationParameterEnricher();
            try (final Jsonb jsonb = newJsonb()) {
                Stream.concat(finder.findAnnotatedClasses(Validation.class).stream().map(validation -> {
                    final Validation val = validation.getAnnotation(Validation.class);
                    return createConstraint(validation, val);

                }), finder
                        .findAnnotatedClasses(Validations.class)
                        .stream()
                        .flatMap(validations -> Stream
                                .of(validations.getAnnotation(Validations.class).value())
                                .map(validation -> createConstraint(validations, validation))))
                        .sorted((o1, o2) -> {
                            final int types = Stream
                                    .of(o1.types)
                                    .map(Class::getName)
                                    .collect(joining("/"))
                                    .compareTo(Stream.of(o2.types).map(Class::getName).collect(joining("/")));
                            if (types == 0) {
                                return o1.name.compareTo(o2.name);
                            }
                            return types;
                        })
                        .forEach(constraint -> {
                            stream.println();
                            stream.println("= " + capitalizeWords(constraint.name));
                            stream.println();
                            stream.println(constraint.description);
                            stream.println();
                            stream.println("- API: `@" + constraint.marker.getName() + "`");
                            stream.println("- Name: `" + constraint.name + "`");
                            stream.println("- Parameter Type: `" + constraint.paramType + "`");
                            stream.println("- Supported Types:");
                            Stream
                                    .of(constraint.types)
                                    .map(Class::getName)
                                    .map(it -> "-- `" + it + "`")
                                    .forEach(stream::println);
                            stream.println("- Sample:");
                            stream.println();
                            stream.println("[source,js]");
                            stream.println("----");
                            stream
                                    .println(jsonb
                                            .toJson(new TreeMap<>(enricher
                                                    .onParameterAnnotation("test", constraint.types[0],
                                                            generateAnnotation(constraint.marker))))
                                            .replace("tcomp::", ""));
                            stream.println("----");
                            stream.println();
                        });
            }
            stream.println();

        }
    }

    private static Constraint createConstraint(final Class<?> validation, final Validation val) {
        return new Constraint(val.name(), val.expectedTypes(), getParamType(validation), validation,
                extractDoc(validation));
    }

    private static String extractDoc(final Class<?> validation) {
        return ofNullable(validation.getAnnotation(Documentation.class))
                .map(Documentation::value)
                .orElse("-")
                .replace("|", "\\|");
    }

    private static String getParamType(final Class<?> validation) {
        try {
            final Class<?> returnType = validation.getMethod("value").getReturnType();
            return returnType.getName().toLowerCase(ENGLISH);
        } catch (final NoSuchMethodException e) {
            return "-";
        }
    }

    // generate a "mock" annotation to be able to generate sample metadata - mainly
    // for @Ui
    private static <T extends Annotation> T generateAnnotation(final Class<?> type) {
        return (T) Proxy
                .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { type },
                        (proxy, method, args) -> {
                            if ("annotationType".equals(method.getName())
                                    && Annotation.class == method.getDeclaringClass()) {
                                return type;
                            }
                            if (method.isDefault()) {
                                return Defaults
                                        .of(method.getDeclaringClass())
                                        .unreflectSpecial(method, method.getDeclaringClass())
                                        .bindTo(proxy)
                                        .invokeWithArguments(args);
                            }
                            final Class<?> returnType = method.getReturnType();
                            if (int.class == returnType) {
                                return 1234;
                            }
                            if (boolean.class == returnType) {
                                return false;
                            }
                            if (double.class == returnType) {
                                return 12.34;
                            }
                            if (String.class == returnType) {
                                return "test";
                            }
                            if (Class.class == returnType) {
                                return AutoLayout.class;
                            }
                            if (String[].class == returnType) {
                                return new String[] { "value1", "value2" };
                            }
                            if (ActiveIf.EvaluationStrategy.class == returnType) {
                                return ActiveIf.EvaluationStrategy.DEFAULT;
                            }
                            if (ActiveIf.EvaluationStrategyOption[].class == returnType) {
                                return new ActiveIf.EvaluationStrategyOption[0];
                            }
                            if (ActiveIfs.Operator.class == returnType) {
                                return ActiveIfs.Operator.AND;
                            }
                            if (Structure.Type.class == returnType) {
                                return Structure.Type.IN;
                            }
                            if (GridLayout.Row[].class == returnType) {
                                return new GridLayout.Row[] { new GridLayout.Row() {

                                    @Override
                                    public Class<? extends Annotation> annotationType() {
                                        return GridLayout.Row.class;
                                    }

                                    @Override
                                    public String[] value() {
                                        return new String[] { "first" };
                                    }
                                }, new GridLayout.Row() {

                                    @Override
                                    public Class<? extends Annotation> annotationType() {
                                        return GridLayout.Row.class;
                                    }

                                    @Override
                                    public String[] value() {
                                        return new String[] { "second", "third" };
                                    }
                                } };
                            }
                            if (ActiveIf[].class == returnType) {
                                return new ActiveIf[] { new ActiveIf() {

                                    @Override
                                    public EvaluationStrategyOption[] evaluationStrategyOptions() {
                                        return new EvaluationStrategyOption[0];
                                    }

                                    @Override
                                    public String target() {
                                        return "sibling1";
                                    }

                                    @Override
                                    public String[] value() {
                                        return new String[] { "value1", "value2" };
                                    }

                                    @Override
                                    public boolean negate() {
                                        return false;
                                    }

                                    @Override
                                    public EvaluationStrategy evaluationStrategy() {
                                        return EvaluationStrategy.DEFAULT;
                                    }

                                    @Override
                                    public Class<? extends Annotation> annotationType() {
                                        return ActiveIf.class;
                                    }
                                }, new ActiveIf() {

                                    @Override
                                    public EvaluationStrategyOption[] evaluationStrategyOptions() {
                                        return new EvaluationStrategyOption[0];
                                    }

                                    @Override
                                    public String target() {
                                        return "../../other";
                                    }

                                    @Override
                                    public String[] value() {
                                        return new String[] { "SELECTED" };
                                    }

                                    @Override
                                    public boolean negate() {
                                        return true;
                                    }

                                    @Override
                                    public EvaluationStrategy evaluationStrategy() {
                                        return EvaluationStrategy.LENGTH;
                                    }

                                    @Override
                                    public Class<? extends Annotation> annotationType() {
                                        return ActiveIf.class;
                                    }
                                } };
                            }
                            if (GridLayout[].class == returnType) {
                                return new GridLayout[] { new GridLayout() {

                                    @Override
                                    public Row[] value() {
                                        return new Row[] { new Row() {

                                            @Override
                                            public Class<? extends Annotation> annotationType() {
                                                return Row.class;
                                            }

                                            @Override
                                            public String[] value() {
                                                return new String[] { "first" };
                                            }
                                        }, new Row() {

                                            @Override
                                            public Class<? extends Annotation> annotationType() {
                                                return Row.class;
                                            }

                                            @Override
                                            public String[] value() {
                                                return new String[] { "second", "third" };
                                            }
                                        } };
                                    }

                                    @Override
                                    public String[] names() {
                                        return new String[] { FormType.MAIN };
                                    }

                                    @Override
                                    public Class<? extends Annotation> annotationType() {
                                        return GridLayout.class;
                                    }
                                }, new GridLayout() {

                                    @Override
                                    public Row[] value() {
                                        return new Row[] { new Row() {

                                            @Override
                                            public Class<? extends Annotation> annotationType() {
                                                return Row.class;
                                            }

                                            @Override
                                            public String[] value() {
                                                return new String[] { "another" };
                                            }
                                        } };
                                    }

                                    @Override
                                    public String[] names() {
                                        return new String[] { FormType.ADVANCED };
                                    }

                                    @Override
                                    public Class<? extends Annotation> annotationType() {
                                        return GridLayout.class;
                                    }
                                } };
                            }
                            return null;
                        });
    }

    private static Jsonb newJsonb() {
        return JsonbBuilder
                .create(new JsonbConfig()
                        .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL)
                        .withFormatting(true));
    }

    @RequiredArgsConstructor
    private static final class Constraint {

        private final String name;

        private final Class<?>[] types;

        private final String paramType;

        private final Class<?> marker;

        private final String description;
    }

    @Data
    public static class JiraVersion {

        private String id;

        private String name;

        private boolean released;

        private boolean archived;

        private long projectId;
    }

    @Data
    public static class JiraIssues {

        private long startAt;

        private long maxResults;

        private long total;

        private Collection<JiraIssue> issues;
    }

    @Data
    public static class IssueType {

        private String name;
    }

    @Data
    public static class JiraIssue {

        private String id;

        private String key;

        private Fields fields;
    }

    @Data
    public static class Fields {

        private String summary;

        private String description;

        private IssueType issuetype;

        private Status status;

        private Collection<JiraVersion> fixVersions;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentationItem {

        private int index;

        private String icon;

        private String label;

        private String description;

        private String link;
    }

    @Data
    public static class Status {

        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Icon {

        private String name;

        private String path;

        private boolean legacy;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {

        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable<T> {

        void run() throws Exception;
    }

    private static class Tasks implements AutoCloseable {

        private final ForkJoinPool executorService;

        private final Collection<Future<?>> tasks = new ArrayList<>();

        private final Collection<Throwable> errors = new ArrayList<>();

        public Tasks() {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            executorService = new ForkJoinPool(Math.max(4, Runtime.getRuntime().availableProcessors() * 8),
                    p -> new ForkJoinWorkerThread(p) {

                        {
                            setContextClassLoader(loader);
                        }
                    }, (r, executor) -> errors.add(new IllegalStateException("Task rejected: " + r)), false);
        }

        <T> CompletionStage<T> register(final ThrowingSupplier<T> runnable) {
            final CompletableFuture<T> out = new CompletableFuture<T>() {

                @Override // register it as part of the await logic
                public CompletableFuture<Void> thenAccept(final Consumer<? super T> action) {
                    final ManualFuture<Void> future = new ManualFuture<>();
                    tasks.add(future);
                    return super.thenAccept(a -> {
                        try {
                            action.accept(a);
                        } finally {
                            future.onSuccess(null);
                        }
                    });
                }

                @Override
                public <U> CompletableFuture<U> thenApply(final Function<? super T, ? extends U> fn) {
                    final ManualFuture<Void> future = new ManualFuture<>();
                    tasks.add(future);
                    return super.thenApply(a -> {
                        try {
                            return fn.apply(a);
                        } finally {
                            future.onSuccess(null);
                        }
                    });
                }
            };
            tasks.add(executorService.submit(() -> {
                try {
                    out.complete(runnable.get());
                } catch (final Exception e) {
                    out.completeExceptionally(e);
                    errors.add(e);
                    throw new IllegalStateException(e);
                }
            }));
            return out;
        }

        void register(final ThrowingRunnable runnable) {
            register(() -> {
                runnable.run();
                return null;
            });
        }

        @Override
        public void close() {
            executorService.shutdown();
            tasks.forEach(it -> {
                try {
                    it.get();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (final ExecutionException e) {
                    final Throwable cause = e.getCause();
                    errors.add(cause);
                    throw new IllegalStateException(cause);
                }
            });
            try {
                if (!executorService.awaitTermination(5, SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!errors.isEmpty()) {
                throw new IllegalStateException(errors.stream().map(Throwable::getMessage).collect(joining("\n")));
            }
        }
    }

    private static class ManualFuture<T> implements Future<T> {

        private final CountDownLatch latch = new CountDownLatch(1);

        private final AtomicBoolean done = new AtomicBoolean(false);

        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private volatile T instance;

        private volatile Exception error;

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            latch.countDown();
            cancelled.set(true);
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return done.get();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            latch.await();
            return getResult();
        }

        @Override
        public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException {
            latch.await(timeout, unit);
            return getResult();
        }

        private T getResult() throws ExecutionException {
            if (error != null) {
                throw new ExecutionException(error);
            }
            return instance;
        }

        public void onFailure(final Exception o) {
            error = o;
            done.set(true);
            latch.countDown();
        }

        public void onSuccess(final T o) {
            instance = o;
            done.set(true);
            latch.countDown();
        }
    }
}
