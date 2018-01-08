/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.starter.server.configuration;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.Configuration;

@ApplicationScoped
@Configuration(prefix = "talend.component.starter.")
public interface StarterConfiguration {

    @ConfigProperty(name = "dictionary.credentials", defaultValue = "password;passwd;pass;credential;token;secret",
            converter = ConfigurationConverters.SetConverter.class)
    Set<String> dictionaryCredentials();

    @ConfigProperty(name = "work.dir", defaultValue = "${java.io.tmpdir}")
    String workDir();

    @ConfigProperty(name = "github.repository.pattern", defaultValue = "https://github.com/%s/%s")
    String githubRepository();

    @ConfigProperty(name = "github.api.base", defaultValue = "https://api.github.com")
    String githubBaseApi();

    @ConfigProperty(name = "github.api.project.user.create.path", defaultValue = "/user/repos")
    String githubCreateProjectPath();

    @ConfigProperty(name = "github.api.project.org.create.path", defaultValue = "/orgs/{name}/repos")
    String githubOrgCreateProjectPath();

    @ConfigProperty(name = "github.api.project.create.method", defaultValue = "POST")
    String githubCreateProjectMethod();

    @ConfigProperty(name = "security.csp", defaultValue = "default-src 'self' data: ; frame-ancestors 'none'")
    String csp();
}
