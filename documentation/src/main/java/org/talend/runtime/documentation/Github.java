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

import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbProperty;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.cxf.transport.common.gzip.GZIPFeature;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.talend.sdk.component.maven.MavenDecrypter;
import org.talend.sdk.component.maven.Server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class Github {

    private final String user;

    private final String password;

    private final GenericType<Collection<GithubContributor>> responseType =
            new GenericType<Collection<GithubContributor>>() {
            };

    public Collection<Contributor> load() {
        final String token =
                "Basic " + Base64.getEncoder().encodeToString((user + ':' + password).getBytes(StandardCharsets.UTF_8));

        final Client client =
                ClientBuilder.newClient().register(new JsonbJaxrsProvider<>()).register(new GZIPFeature());
        final WebTarget gravatarBase = client.target(Gravatars.GRAVATAR_BASE);
        final ExecutorService pool =
                Executors.newCachedThreadPool(r -> new Thread(r, Github.class.getName() + "-" + r.hashCode()));
        try { // we try to parallelize as much as possible remote calls
            final CompletableFuture<List<GithubContributor>>[] contributorLookups = toArray(Stream
                    .of("component-api", "component-runtime")
                    .map(repo -> toStage(pool,
                            () -> contributors(client, token,
                                    "https://api.github" + ".com/repos/talend" + "/" + repo + "/contributors")
                                            .collect(toList()))));

            return allOf(contributorLookups)
                    .thenApply(ignored -> toArray(Stream
                            .of(contributorLookups)
                            .flatMap(it -> getResult(it).stream())
                            .collect(toMap(e -> normalizeLogin(e.login), identity(), (c1, c2) -> {
                                c1.contributions += c2.contributions;
                                return c1;
                            }))
                            .values()
                            .stream()
                            .map(contributor -> toStage(pool,
                                    () -> loadContributor(token, client, gravatarBase, contributor)))))
                    .thenCompose(contribs -> allOf(contribs)
                            .thenApply(ignored -> Stream
                                    .of(contribs)
                                    .map(this::getResult)
                                    .filter(Objects::nonNull)
                                    .sorted(comparing(Contributor::getCommits).reversed())
                                    .collect(toList())))
                    .get();
        } catch (final ExecutionException ee) {
            if (WebApplicationException.class.isInstance(ee.getCause())) {
                final Response response = WebApplicationException.class.cast(ee.getCause()).getResponse();
                if (response != null && response.getEntity() != null) {
                    log.error(response.readEntity(String.class));
                }
            }
            throw new IllegalStateException(ee.getCause());
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            client.close();
            pool.shutdownNow();
        }
    }

    private <T> T getResult(final Future<T> f) {
        try {
            return f.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private static <T> CompletableFuture<T> toStage(final ExecutorService pool, final Supplier<T> supplier) {
        final CompletableFuture<T> stage = new CompletableFuture<>();
        pool.submit(() -> {
            try {
                stage.complete(supplier.get());
            } catch (final Exception e) {
                stage.completeExceptionally(e);
            }
        });
        return stage;
    }

    private static <T> CompletableFuture<T>[] toArray(final Stream<CompletableFuture<T>> stream) {
        return stream.toArray(CompletableFuture[]::new);
    }

    private Contributor loadContributor(final String token, final Client client, final WebTarget gravatarBase,
            final GithubContributor contributor) {
        if (contributor.url == null) { // anon contributor

            try {
                final Contributor gravatar = Gravatars.loadGravatar(gravatarBase, contributor.email);
                return Contributor
                        .builder()
                        .name(contributor.name)
                        .commits(contributor.contributions)
                        .description(gravatar.getDescription())
                        .gravatar(gravatar.getGravatar())
                        .build();
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
                return new Contributor(contributor.email, contributor.email, "", Gravatars.url(contributor.email),
                        contributor.contributions);
            }
        }
        final GithubUser user = client
                .target(contributor.url)
                .request(APPLICATION_JSON_TYPE)
                .header("Authorization", token)
                .get(GithubUser.class);
        return Contributor
                .builder()
                .id(contributor.login)
                .name(ofNullable(user.name).orElse(contributor.name))
                .description((user.bio == null ? "" : user.bio) + (user.blog != null && !user.blog.trim().isEmpty()
                        && (user.bio == null || !user.bio.contains(user.blog)) ? "\n\nBlog: " + user.blog : ""))
                .commits(contributor.contributions)
                .gravatar(ofNullable(contributor.avatarUrl).orElseGet(() -> {
                    final String gravatarId =
                            contributor.gravatarId == null || contributor.gravatarId.isEmpty() ? contributor.email
                                    : contributor.gravatarId;
                    try {
                        return Gravatars.loadGravatar(gravatarBase, gravatarId).getGravatar();
                    } catch (final Exception e) {
                        log.warn(e.getMessage(), e);
                        return Gravatars.url(gravatarId);
                    }
                }))
                .build();
    }

    // handle duplicates
    private String normalizeLogin(final String login) {
        if (login != null) {
            switch (login.toLowerCase(ROOT)) {
            case "jso-technologies":
                return "jsomsanith";
            default:
            }
        }
        return login;
    }

    private Stream<GithubContributor> contributors(final Client client, final String token, final String url) {
        return Stream
                .of(client
                        .target(url)
                        .queryParam("per_page", 100)
                        .request(APPLICATION_JSON_TYPE)
                        .header("Authorization", token)
                        .get())
                .flatMap(response -> {
                    final String link = response.getHeaderString("Link");
                    if (response.getStatus() > 299) {
                        throw new IllegalStateException("Invalid response: HTP " + response.getStatus() + " / "
                                + response.readEntity(String.class));
                    }
                    final Stream<GithubContributor> pageContributors = response.readEntity(responseType).stream();
                    if (link == null) {
                        return pageContributors;
                    }
                    return Stream
                            .concat(pageContributors,
                                    Stream
                                            .of(link.split(","))
                                            .map(String::trim)
                                            .filter(s -> s.endsWith("rel=\"next\""))
                                            .flatMap(l -> {
                                                final int from = l.indexOf('<');
                                                final int to = l.indexOf('>');
                                                return contributors(client, token, l.substring(from + 1, to));
                                            }));
                });
    }

    public static void main(final String[] args) {
        final Server server = new MavenDecrypter().find("github");
        final Collection<Contributor> contributors = new Github(server.getUsername(), server.getPassword()).load();
        System.out.println(contributors);
    }

    @Data
    public static class GithubUser {

        private String name;

        private String bio;

        private String blog;
    }

    @Data
    public static class GithubContributor {

        private String login;

        private String url;

        private String email;

        private String name;

        private int contributions;

        @JsonbProperty("avatar_url")
        private String avatarUrl;

        @JsonbProperty("gravatar_id")
        private String gravatarId;
    }
}
