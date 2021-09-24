/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenWorkingSession;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenChecksumPolicy;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepositories;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepository;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenUpdatePolicy;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.AcceptScopesStrategy;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.MavenResolutionStrategy;
import org.jboss.shrinkwrap.resolver.impl.maven.PomEquippedResolveStageBaseImpl;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Dependencies {

    private static final ConcurrentMap<MavenDependency, URL[]> CACHE = new ConcurrentHashMap<>();

    private static final List<MavenRemoteRepository> REPOSITORIES = findRepositories();

    private static List<MavenRemoteRepository> findRepositories() {
        final List<MavenRemoteRepository> repositories = new ArrayList<>();

        final String repos = System.getProperty("talend.component.junit.maven.repositories");
        if (repos != null && !repos.isEmpty()) {
            for (final String repo : repos.split(",")) {
                final String[] parts = repo.split("::");
                final MavenRemoteRepository repository = MavenRemoteRepositories
                        .createRemoteRepository(
                                parts.length == 1 ? "repo_" + parts[0].replace(':', '_').replace('/', '_') : parts[0],
                                parts.length == 1 ? parts[0] : parts[1], parts.length < 3 ? "default" : parts[2]);
                repository.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_DAILY);
                repository.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE);
                repositories.add(repository);
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
                final PomEquippedResolveStage localPom = Maven.configureResolver().loadPomFromFile(pom);
                final MavenWorkingSession mavenWorkingSession =
                        PomEquippedResolveStageBaseImpl.class.cast(localPom).getMavenWorkingSession();
                try {
                    final Field remoteRepositories =
                            mavenWorkingSession.getClass().getDeclaredField("remoteRepositories");
                    if (!remoteRepositories.isAccessible()) {
                        remoteRepositories.setAccessible(true);
                    }
                    final List<RemoteRepository> pomRepos =
                            List.class.cast(remoteRepositories.get(mavenWorkingSession));
                    if (pomRepos != null && !pomRepos.isEmpty()) {
                        pomRepos.forEach(r -> {
                            final MavenRemoteRepository repository =
                                    MavenRemoteRepositories.createRemoteRepository(r.getId(), r.getUrl(), "default");
                            repository.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_DAILY);
                            repository.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE);
                            repositories.add(repository);
                        });
                    }
                } catch (final Exception e) {
                    throw new IllegalStateException("Check your shrinkwrap maven version", e);
                }
            }
        }
        if (!Boolean.getBoolean("talend.component.junit.maven.central.skip")) {
            final MavenRemoteRepository central = MavenRemoteRepositories
                    .createRemoteRepository("central_test_default", "https://repo.maven.apache.org/maven2/", "default");
            central.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_WARN);
            central.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER);
            repositories.add(central);
        }

        return repositories;
    }

    private static final MavenResolutionStrategy STRATEGY =
            new AcceptScopesStrategy(ScopeType.COMPILE, ScopeType.RUNTIME, ScopeType.IMPORT);

    public static URL[] resolve(final MavenDependency dep) {
        return CACHE.computeIfAbsent(dep, d -> {
            final ConfigurableMavenResolverSystem resolver = Maven
                    .configureResolver()
                    .withClassPathResolution(true)
                    .useLegacyLocalRepo(true)
                    .workOffline(Boolean.getBoolean("talend.component.junit.maven.offline"));

            REPOSITORIES.forEach(resolver::withRemoteRepo);
            resolver.addDependency(dep);
            final MavenStrategyStage strategyStage = resolver.resolve();
            final MavenFormatStage formatStage = strategyStage.using(STRATEGY);
            return Stream.of(formatStage.asFile()).distinct().map(f -> {
                try {
                    return f.toURI().toURL();
                } catch (final MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            }).toArray(URL[]::new);
        });
    }
}
