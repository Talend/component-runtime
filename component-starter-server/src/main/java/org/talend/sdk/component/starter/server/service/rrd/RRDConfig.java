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
package org.talend.sdk.component.starter.server.service.rrd;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;

@Getter
@ApplicationScoped
public class RRDConfig {

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.skip", defaultValue = "false")
    private Boolean skipped;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.location")
    private Optional<String> location;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.export")
    private Optional<String> export;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.endpoint.concurrent", defaultValue = "2")
    private Integer acceptedConcurrentCalls;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.fetch.max", defaultValue = "1024")
    private Integer maxFetchPoints;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.heartbeat", defaultValue = "600")
    private Integer heartbeat;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.synchronization.period", defaultValue = "300")
    private Integer syncPeriod;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.synchronization.threads", defaultValue = "1")
    private Integer synchronizerThreads;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.dump.timeout", defaultValue = "3600000") // 1h
    private Integer autoDumpTimeout;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.steps", defaultValue = "300")
    private Integer steps;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.archive.xff", defaultValue = "0.")
    private Float archiveXff;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.archive.rows", defaultValue = "600,700,775,797")
    private List<Integer> archiveRows;

    @Inject
    @ConfigProperty(name = "talend.component.starter.rrd.archive.steps", defaultValue = "1,6,24,288")
    private List<Integer> archiveSteps;
}
