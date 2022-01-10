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
package org.talend.sdk.component.remoteengine.customizer;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.remoteengine.customizer.model.DockerConfiguration;
import org.talend.sdk.component.remoteengine.customizer.model.ImageType;
import org.talend.sdk.component.remoteengine.customizer.model.RegistryConfiguration;
import org.talend.sdk.component.remoteengine.customizer.service.ConnectorLoader;
import org.talend.sdk.component.remoteengine.customizer.task.RemoteEngineCustomizer;
import org.tomitribe.crest.Main;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;
import org.tomitribe.crest.environments.Environment;
import org.tomitribe.crest.environments.SystemEnvironment;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Cli {

    public static void main(final String[] args) throws Exception {
        ofNullable(run(args)).ifPresent(System.out::println);
    }

    public static Object run(final String[] args) throws Exception {
        final Map<Class<?>, Object> services = new HashMap<>();
        services.put(ConnectorLoader.class, new ConnectorLoader());
        services.put(RemoteEngineCustomizer.class, new RemoteEngineCustomizer());
        Map<String, String> env = new HashMap<>();
        env.putAll(System.getenv());
        env.put("NOCOLOR", "true");
        env.put("NOLESS", "true");
        Environment.ENVIRONMENT_THREAD_LOCAL.set(new MyEnv(services, env));
        try {
            return new Main(Cli.class).exec(args);
        } finally {
            Environment.ENVIRONMENT_THREAD_LOCAL.remove();
        }
    }

    public static class MyEnv extends SystemEnvironment {

        final Map<String, String> env;

        public MyEnv(final Map<Class<?>, Object> services, final Map<String, String> env) {
            super(services);
            this.env = env;
        }

        @Override
        public Map<String, String> getEnv() {
            return env;
        }
    }

    // CHECKSTYLE:OFF
    @Command("register-component-archive")
    public static void registerComponents(@Option("remote-engine-dir") final String remoteEngineDirConf,
            @Option("work-dir") @Default("${java.io.tmpdir}/remote-engine-customizer") final String workDirConf,
            @Option("cache-dir") @Default("${remote.engine.dir}/.remote_engine_customizer/cache") final String cacheDirConf,
            @Option("base-image") @Default("auto") final String baseImageConf,
            @Option("target-image") @Default("auto") final String targetImageConf,
            @Option("component-archive") final Collection<String> carPaths,
            @Option("from-image-type") @Default("AUTO") final ImageType fromImageType,
            @Option("to-image-type") @Default("DOCKER") final ImageType targetImageType,
            @Option("docker-configuration-") final DockerConfiguration dockerConfiguration,
            @Option("registry-configuration-") final RegistryConfiguration registryConfiguration,
            @Option("update-original-docker-compose") @Default("true") final boolean updateOriginalFile,
            final ConnectorLoader connectorLoader, final RemoteEngineCustomizer remoteEngineCustomizer) {
        remoteEngineCustomizer
                .registerComponents(remoteEngineDirConf, workDirConf, cacheDirConf, baseImageConf, targetImageConf,
                        carPaths, fromImageType, targetImageType, dockerConfiguration, registryConfiguration,
                        connectorLoader, updateOriginalFile);
    }
}
