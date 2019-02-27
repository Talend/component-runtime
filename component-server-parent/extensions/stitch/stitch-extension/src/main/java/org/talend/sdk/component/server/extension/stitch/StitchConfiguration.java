/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.server.extension.api.configuration.Documentation;

import lombok.Data;

@Data
@ApplicationScoped
public class StitchConfiguration {

    @Inject
    @Documentation("HTTP connection timeout to Stitch server.")
    @ConfigProperty(name = "talend.server.extension.stitch.client.timeout.connect", defaultValue = "30000")
    private Long connectTimeout;

    @Inject
    @Documentation("HTTP read timeout to Stitch server.")
    @ConfigProperty(name = "talend.server.extension.stitch.client.timeout.read", defaultValue = "30000")
    private Long readTimeout;

    @Inject
    @Documentation("How long the initialization can wait before ignoring Stitch data.")
    @ConfigProperty(name = "talend.server.extension.stitch.init.timeout", defaultValue = "180000")
    private Long initTimeout;

    @Inject
    @Documentation("How many threads can be allocated to stitch extension at startup.")
    @ConfigProperty(name = "talend.server.extension.stitch.client.parallelism", defaultValue = "-1")
    private Integer parallelism;

    @Inject
    @Documentation("How many threads can be allocated to stitch extension at startup.")
    @ConfigProperty(name = "talend.server.extension.stitch.client.retries", defaultValue = "1")
    private Integer retries;

    @Inject
    @Documentation("Base Stitch API URL.")
    @ConfigProperty(name = "talend.server.extension.stitch.client.base",
            defaultValue = "https://api.stitchdata.com/v4/")
    private String base;

    @Inject
    @Documentation("Stitch Token to be able to connect to the server, if not set Stitch extension is skipped.")
    @ConfigProperty(name = "talend.server.extension.stitch.token")
    private Optional<String> token;
}
