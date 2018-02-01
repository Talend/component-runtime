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
package org.talend.sdk.component.junit.environment;

import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolveStageBase;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.AcceptScopesStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.MavenResolutionStrategy;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Dependencies {

    private static final ConcurrentMap<String, URL[]> CACHE = new ConcurrentHashMap<>();

    private static final MavenResolveStageBase<?, ?, ?> RESOLVER = createResolver();

    private static MavenResolveStageBase<?, ?, ?> createResolver() {
        ConfigurableMavenResolverSystem resolverSystem =
                Maven.configureResolver().withClassPathResolution(true).workOffline(
                        Boolean.getBoolean("talend.component.junit.maven.offline"));

        final String repos = System.getProperty("talend.component.junit.maven.repositories");
        if (repos != null && !repos.isEmpty()) {
            for (final String repo : repos.split(",")) {
                final String[] parts = repo.split("::");
                resolverSystem = resolverSystem.withRemoteRepo(parts[0], parts[1], parts[2]);
            }
        } else if (!Boolean.getBoolean("talend.component.junit.maven.skipPomReading")) {
            // try to grab from the root pom the repos - simplifying maven parsing for speed reasons and enough
            // generally

            File current = new File(".").getAbsoluteFile();
            while (new File(current, "pom.xml").exists()) {
                final File parent = current.getParentFile();
                if (parent != null && new File(parent, "pom.xml").exists()) {
                    current = parent;
                    continue;
                }
                break;
            }
            final File pom = new File(current, "pom.xml");
            if (pom.exists()) {
                return resolverSystem.loadPomFromFile(pom);
            }
        }

        return resolverSystem;
    }

    private static final MavenResolutionStrategy STRATEGY =
            new AcceptScopesStrategy(ScopeType.COMPILE, ScopeType.RUNTIME);

    public static URL[] resolve(final String dep) {
        return CACHE.computeIfAbsent(dep,
                d -> Stream.of(RESOLVER.resolve(d).using(STRATEGY).asFile()).distinct().map(f -> {
                    try {
                        return f.toURI().toURL();
                    } catch (final MalformedURLException e) {
                        throw new IllegalStateException(e);
                    }
                }).toArray(URL[]::new));
    }
}
