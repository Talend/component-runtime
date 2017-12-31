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

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.ziplock.JarLocation.jarLocation;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.stream.StreamSupport;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.johnzon.jaxrs.JohnzonProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Gravatars {

    private static final String GRAVATAR_BASE = "https://fr.gravatar.com/";

    private static Contributor singleLoad(final WebTarget target, final String input) throws IOException {
        try {
            return ofNullable(loadGravatar(target, input)).orElse(null);
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    private static Contributor loadGravatar(final WebTarget target, final String input) throws IOException {
        final String hash = gravatarHash(input);
        final Response gravatar = target.path(hash + ".json").request(APPLICATION_JSON_TYPE).get();
        final String gravatarUrl = "https://www.gravatar.com/avatar/" + hash + "?s=140";
        return ofNullable(gravatar)
                .filter(r -> r.getStatus() == HttpURLConnection.HTTP_OK)
                .map(r -> r.readEntity(Gravatar.class).getEntry())
                .map(e -> e[0])
                .map(e -> Contributor
                        .builder()
                        .id(e.getId())
                        .name(ofNullable(e.getName())
                                .map(n -> ofNullable(n.getFormatted()).orElse(ofNullable(n.getGivenName()).orElse("")
                                        + ofNullable(n.getFamilyName()).orElse("")))
                                .orElseGet(() -> ofNullable(e.getDisplayName())
                                        .orElse(ofNullable(e.getPreferredUsername()).orElse(input))))
                        .description(ofNullable(e.getAboutMe()).orElse(""))
                        .gravatar(gravatarUrl)
                        .build())
                .orElse(Contributor.builder().name(input).id(input).description("").gravatar(gravatarUrl).build());
    }

    public static Collection<Contributor> load() throws IOException {
        // start by doing a gitlog to get all contributors
        final Map<String, Integer> mails;
        try {
            final File gitRoot = jarLocation(Gravatars.class).getParentFile().getParentFile().getParentFile();
            mails = StreamSupport
                    .stream(Git.open(gitRoot).log().call().spliterator(), false)
                    .map(commit -> Pair.of(commit.getAuthorIdent().getEmailAddress(), 1))
                    .collect(toMap(Pair::getKey, Pair::getValue, (v1, v2) -> v1 + v2));
        } catch (final GitAPIException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("Contributor mails: " + mails);

        // merge the multiple names for committers - if committed with multiple mails
        final Set<String> keys = new HashSet<>(mails.keySet());
        keys.forEach(mail -> {
            final int prefix = mail.indexOf("@");
            if (prefix < 0) {
                return;
            }
            final String filter = mail.substring(0, prefix) + '@';
            final List<String> toAggregate =
                    mails.keySet().stream().filter(k -> k.startsWith(filter)).collect(toList());
            if (toAggregate.size() <= 1) {
                return;
            }
            final int count = mails
                    .entrySet()
                    .stream()
                    .filter(e -> toAggregate.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .reduce(0, (a, e) -> a + e);
            toAggregate.forEach(mails::remove);
            toAggregate.sort((mail1, mail2) -> {
                if (mail1.endsWith("@apache.org")) {
                    return 1;
                }
                if (mail2.endsWith("@apache.org")) {
                    return -1;
                }
                if (mail2.endsWith("@talend.com")) {
                    return 1;
                }
                if (mail1.endsWith("@talend.com")) {
                    return -1;
                }
                return mail1.compareTo(mail2);
            });
            mails.put(toAggregate.get(0), count);
        });
        System.out.println("Unified contributors mails: " + mails);

        // then get all their images
        final WebTarget target = ClientBuilder.newClient().register(new JohnzonProvider<>()).target(GRAVATAR_BASE);
        final ForkJoinPool pool = new ForkJoinPool(Math.min(16, mails.size()));
        try {
            final List<Contributor> contributors = pool.submit(() -> mails.entrySet().stream().parallel().map(mail -> {
                try {
                    final Contributor contributor = singleLoad(target, mail.getKey());
                    if (contributor == null) {
                        return createDefault(mail);
                    }
                    contributor.setCommits(mail.getValue());
                    return contributor;
                } catch (final IOException e) {
                    return createDefault(mail);
                }
            }).collect(toList())).get(15, MINUTES);
            contributors.sort(Comparator.comparing(Contributor::getCommits).reversed().thenComparing(
                    comparing(Contributor::getName)));
            return contributors;
        } catch (final InterruptedException e) {
            Thread.interrupted();
            return emptyList();
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } catch (final TimeoutException e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(1, MINUTES);
            } catch (final InterruptedException e) {
                Thread.interrupted();
                log.warn(e.getMessage());
            }
        }
    }

    private static Contributor createDefault(final Map.Entry<String, Integer> mail) {
        return new Contributor(mail.getKey(), mail.getKey(), "", gravatarHash(mail.getKey()), mail.getValue());
    }

    private static String gravatarHash(final String mail) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] cp1252s = md.digest(mail.getBytes("CP1252"));
            final StringBuilder sb = new StringBuilder();
            for (final byte anArray : cp1252s) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Data
    @Builder
    public static class Link {

        private String name;

        private String url;
    }

    @Data
    @Builder
    public static class Contributor {

        private String id;

        private String name;

        private String description;

        private String gravatar;

        private int commits;
    }

    @Data
    public static class GravatarName {

        private String formatted;

        private String givenName;

        private String familyName;
    }

    @Data
    public static class GravatarUrl {

        private String value;

        private String title;
    }

    @Data
    public static class GravatarAccount {

        private String shortname;

        private String url;
    }

    @Data
    public static class Gravatar {

        private GravatarEntry[] entry;
    }

    @Data
    public static class GravatarEntry {

        private String id;

        private String hash;

        private String aboutMe;

        private String requestHash;

        private String profileUrl;

        private String preferredUsername;

        private String thumbnailUrl;

        private GravatarName name;

        private GravatarUrl[] urls;

        private GravatarAccount[] accounts;

        private String displayName;
    }
}
