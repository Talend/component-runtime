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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.security;

import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DockerHostNameSanitizer {

    public String sanitizeDockerHostname(final String remoteHost) {
        // pattern we want to match is <folder>_<service>_<number>.<folder>_<network>
        final String[] segments = remoteHost.split("\\.");
        if (segments.length == 2) {
            final String[] firstSegments = segments[0].split("_");
            if (firstSegments.length >= 3) {
                int instanceNumberIndex = -1;
                for (int i = 1; i <= 2; i++) { // docker-compose 1.23 changed the behavior adding a has as on swarm
                    try {
                        Integer.parseInt(firstSegments[firstSegments.length - i]);
                        instanceNumberIndex = i;
                        break;
                    } catch (final NumberFormatException nfe) {
                        // no-op
                    }
                }
                if (instanceNumberIndex < 0) { // not found
                    return remoteHost;
                }

                final String[] secondPathSegments = segments[1].split("_");
                if (secondPathSegments.length >= 2 && secondPathSegments[0].equalsIgnoreCase(firstSegments[0])) {
                    final int commonSegments = countCommonSegments(firstSegments, secondPathSegments);
                    return Stream
                            .of(firstSegments)
                            .skip(commonSegments)
                            .limit(firstSegments.length - commonSegments - instanceNumberIndex)
                            .collect(joining("_"));
                }
            }
        }
        return remoteHost;
    }

    public String sanitizeWeaveHostname(final String remoteHost) {
        // pattern we want to match is <prefix>_<service>_<number>.<suffix>
        final String[] segments = remoteHost.split("\\.");
        if (segments.length == 2) {
            final String[] firstSegments = segments[0].split("_");
            if (firstSegments.length >= 3) {
                try {
                    Integer.parseInt(firstSegments[firstSegments.length - 1]);
                } catch (final NumberFormatException nfe) {
                    return remoteHost; // not a docker name
                }
                return Stream.of(firstSegments).skip(1).limit(firstSegments.length - 2).collect(joining("_"));
            }
        }
        return remoteHost;
    }

    private int countCommonSegments(final String[] firstSegments, final String[] secondPathSegments) {
        for (int i = 0; i < Math.min(firstSegments.length, secondPathSegments.length); i++) {
            if (!firstSegments[i].equalsIgnoreCase(secondPathSegments[i])) {
                return i;
            }
        }
        return 0;
    }
}
