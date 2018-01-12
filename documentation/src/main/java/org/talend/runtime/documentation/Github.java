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

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbProperty;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

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

        final Client client = ClientBuilder.newClient().register(new JsonbJaxrsProvider<>());
        final WebTarget gravatarBase = client.target(Gravatars.GRAVATAR_BASE);
        final ForkJoinPool pool = new ForkJoinPool(Math.max(16, Runtime.getRuntime().availableProcessors() * 8));
        try {
            return pool
                    .submit(() -> contributors(client, token,
                            "https://api.github.com/repos/talend/component-runtime/contributors")
                                    .parallel()
                                    .map(contributor -> {
                                        if (contributor.url == null) { // anon contributor

                                            try {
                                                final Contributor gravatar =
                                                        Gravatars.loadGravatar(gravatarBase, contributor.email);
                                                return Contributor
                                                        .builder()
                                                        .name(contributor.name)
                                                        .commits(contributor.contributions)
                                                        .description(gravatar.getDescription())
                                                        .gravatar(gravatar.getGravatar())
                                                        .build();
                                            } catch (final Exception e) {
                                                log.warn(e.getMessage(), e);
                                                return new Contributor(contributor.email, contributor.email, "",
                                                        Gravatars.url(contributor.email), contributor.contributions);
                                            }
                                        }
                                        final GithubUser user =
                                                client.target(contributor.url).request(APPLICATION_JSON_TYPE).get(
                                                        GithubUser.class);
                                        return Contributor
                                                .builder()
                                                .id(contributor.login)
                                                .name(ofNullable(user.name).orElse(contributor.name))
                                                .description((user.bio == null ? "" : user.bio)
                                                        + (user.blog != null && !user.blog.trim().isEmpty()
                                                                && (user.bio == null || !user.bio.contains(user.blog))
                                                                        ? "\n\nBlog: " + user.blog
                                                                        : ""))
                                                .commits(contributor.contributions)
                                                .gravatar(ofNullable(contributor.avatarUrl).orElseGet(() -> {
                                                    final String gravatarId = contributor.gravatarId == null
                                                            || contributor.gravatarId.isEmpty() ? contributor.email
                                                                    : contributor.gravatarId;
                                                    try {
                                                        final Contributor gravatar =
                                                                Gravatars.loadGravatar(gravatarBase, gravatarId);
                                                        return gravatar.getGravatar();
                                                    } catch (final Exception e) {
                                                        log.warn(e.getMessage(), e);
                                                        return Gravatars.url(gravatarId);
                                                    }
                                                }))
                                                .build();
                                    })
                                    .filter(Objects::nonNull)
                                    .collect(toList()))
                    .get(15, MINUTES);
        } catch (final ExecutionException ee) {
            if (WebApplicationException.class.isInstance(ee.getCause())) {
                log.error(WebApplicationException.class.cast(ee.getCause()).getResponse().readEntity(String.class));
            }
            throw new IllegalStateException(ee.getCause());
        } catch (final InterruptedException | TimeoutException e) {
            throw new IllegalStateException(e);
        } finally {
            client.close();
            pool.shutdownNow();
        }
    }

    private Stream<GithubContributor> contributors(final Client client, final String token, final String url) {
        return Stream
                .of(client
                        .target(url)
                        .queryParam("anon", true)
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
                    return Stream.concat(pageContributors,
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
