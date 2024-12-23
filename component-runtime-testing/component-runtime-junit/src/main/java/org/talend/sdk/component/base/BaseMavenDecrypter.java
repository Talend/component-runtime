/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.base;

import java.util.stream.Stream;

import org.talend.sdk.component.api.DecryptedServer;
import org.talend.sdk.component.maven.MavenDecrypter;
import org.talend.sdk.component.maven.Server;

public class BaseMavenDecrypter {

    public Server createInstance(final DecryptedServer config) {
        if (config.alwaysTryLookup()) {
            try {
                return new MavenDecrypter().find(config.value());
            } catch (final IllegalArgumentException iae) {
                if (ifActive(config.conditions())) {
                    throw iae;
                }
            }
        } else if (ifActive(config.conditions())) {
            return new MavenDecrypter().find(config.value());
        }
        final Server server = new Server();
        server.setUsername(config.defaultUsername());
        server.setPassword(config.defaultPassword());
        return server;
    }

    private boolean ifActive(final DecryptedServer.Conditions conditions) {
        final Stream<Boolean> evaluations = Stream.of(conditions.value()).map(c -> {
            final String value = System.getProperty(c.forSystemProperty());
            if (value == null) {
                return c.supportsNull();
            } else {
                return c.expectedValue().equals(value);
            }
        });
        switch (conditions.combination()) {
            case AND:
                return evaluations.allMatch(c -> c);
            case OR:
                return evaluations.anyMatch(c -> c);
            default:
                throw new IllegalArgumentException("Unsupported: " + conditions.combination());
        }
    }
}
