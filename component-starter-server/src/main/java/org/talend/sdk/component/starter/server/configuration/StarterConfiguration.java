/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.configuration;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;

@Getter
@ApplicationScoped
public class StarterConfiguration {

    @Inject
    @ConfigProperty(name = "dictionary.credentials", defaultValue = "password,passwd,pass,credential,token,secret")
    private Set<String> dictionaryCredentials;

    @Inject
    @ConfigProperty(name = "talend.component.starter.talend.component.starter.work.dir",
            defaultValue = "${java.io.tmpdir}")
    private String workDir;

    @Inject
    @ConfigProperty(name = "talend.component.starter.security.csp",
            defaultValue = "default-src 'self' 'unsafe-eval';" +
                    " connect-src 'self' https://unpkg.com;" +
                    " style-src 'self' 'unsafe-inline';" +
                    " script-src 'self' 'unsafe-inline';" +
                    " img-src 'self' data: blob: https://unpkg.com;" +
                    " font-src 'self' data: blob:;" +
                    " frame-ancestors 'none'")
    private String csp;

    @Inject
    @ConfigProperty(name = "talend.component.starter.autorefresh.active", defaultValue = "false")
    private Boolean autoRefresh;

    @Inject
    @ConfigProperty(name = "talend.component.starter.autorefresh.delayMs", defaultValue = "60000")
    private Long refreshDelayMs;
}
