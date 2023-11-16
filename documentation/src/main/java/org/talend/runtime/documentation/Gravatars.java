/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static lombok.AccessLevel.PRIVATE;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Gravatars {

    public static final String GRAVATAR_BASE = "https://fr.gravatar.com/";

    public static String url(final String input) {
        final String hash = gravatarHash(input == null ? "" : input);
        return "https://www.gravatar.com/avatar/" + hash + "?s=140";
    }

    public static Contributor loadGravatar(final WebTarget target, final String input) {
        final String hash = gravatarHash(input);
        final String gravatarUrl = "https://www.gravatar.com/avatar/" + hash + "?s=140";
        final Response gravatar = target.path(hash + ".json").request(APPLICATION_JSON_TYPE).get();
        return ofNullable(gravatar)
                .filter(r -> r.getStatus() == HttpURLConnection.HTTP_OK)
                .map(r -> r.readEntity(Gravatar.class).getEntry())
                .map(e -> e[0])
                .map(e -> Contributor
                        .builder()
                        .id(e.getId())
                        .name(ofNullable(e.getName())
                                .map(n -> ofNullable(n.getFormatted())
                                        .orElse(ofNullable(n.getGivenName()).orElse("")
                                                + ofNullable(n.getFamilyName()).orElse("")))
                                .orElseGet(() -> ofNullable(e.getDisplayName())
                                        .orElse(ofNullable(e.getPreferredUsername()).orElse(input))))
                        .description(ofNullable(e.getAboutMe()).orElse(""))
                        .gravatar(gravatarUrl)
                        .build())
                .orElse(Contributor.builder().name(input).id(input).description("").gravatar(gravatarUrl).build());
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
