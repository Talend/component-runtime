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
package org.talend.sdk.component.server.configuration;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Getter;

@Getter
@ApplicationScoped
public class ComponentServerConfiguration {

    @Inject
    @Documentation("If set it will replace any message for exceptions. Set to `false` to use the actual exception message.")
    @ConfigProperty(name = "talend.component.server.jaxrs.exceptionhandler.defaultMessage", defaultValue = "false")
    private String defaultExceptionMessage;

    @Inject
    @Documentation("The local maven repository used to locate components and their dependencies")
    @ConfigProperty(name = "talend.component.server.maven.repository")
    private Optional<String> mavenRepository;

    // property to list plugins directly by gav. This is nice to set it on the cli but not as a maintenance solution.
    @Inject
    @Documentation("A comma separated list of gav to locate the components")
    @ConfigProperty(name = "talend.component.server.component.coordinates")
    private List<String> componentCoordinates;

    // property to list plugins like in a fatjar, ie value = gav. Nice for assemblies, less for demo/cli usage.
    @Inject
    @Documentation("A property file where the value is a gav of a component to register (complementary with `coordinates`)")
    @ConfigProperty(name = "talend.component.server.component.registry")
    private Optional<String> componentRegistry;

    @Inject
    @Documentation("The size of the execution pool for runtime endpoints.")
    @ConfigProperty(name = "talend.component.server.execution.pool.size", defaultValue = "64")
    private Integer executionPoolSize;

    @Inject
    @Documentation("How long the application waits during shutdown for the execution tasks to complete")
    @ConfigProperty(name = "talend.component.server.execution.pool.wait", defaultValue = "PT10S") // 10s
    private String executionPoolShutdownTimeout;

    @Inject
    @Documentation("How long the read execution endpoint can last (max)")
    @ConfigProperty(name = "talend.component.server.execution.dataset.retriever.timeout", defaultValue = "180") // in
                                                                                                                // sec
    private Long datasetRetrieverTimeout;

    @Inject
    @Documentation("The name used by the brave integration (zipkin)")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.service.name", defaultValue = "component-server")
    private String serviceName;

    @Inject
    @Documentation("The accuracy rate of the sampling.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.rate", defaultValue = "-1.")
    private Float samplerRate;

    @Inject
    @Documentation("The accuracy rate of the sampling for environment endpoints.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.environment.rate", defaultValue = "-1")
    private Float samplerEnvironmentRate;

    @Inject
    @Documentation("The accuracy rate of the sampling for environment endpoints.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.configurationtype.rate",
            defaultValue = "-1")
    private Float samplerConfigurationTypeRate;

    @Inject
    @Documentation("The accuracy rate of the sampling for component endpoints.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.component.rate", defaultValue = "-1")
    private Float samplerComponentRate;

    @Inject
    @Documentation("The accuracy rate of the sampling for documentation endpoint.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.documentation.rate", defaultValue = "-1")
    private Float samplerDocumentationRate;

    @Inject
    @Documentation("The accuracy rate of the sampling for action endpoints.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.action.rate", defaultValue = "-1")
    private Float samplerActionRate;

    @Inject
    @Documentation("The accuracy rate of the sampling for execution endpoints.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.sampling.execution.rate", defaultValue = "1")
    private Float samplerExecutionRate;

    @Inject
    @Documentation("The brave reporter to use to send the spans. Supported values are [log, noop]. When configuration is needed,"
            + "you can use this syntax to configure the repoter if needed: `<name>(config1=value1, config2=value2)`, "
            + "for example: `url(endpoint=http://brave.company.com`.\n\n"
            + "In `log` mode, if environment variable `TRACING_ON` doesn't exist or is set to `false`, `noop` will be selected, "
            + "and is set to `true`, `TRACING_KAFKA_URL`, `TRACING_KAFKA_TOPIC` and `TRACING_SAMPLING_RATE` will configure `kafka` reporter..")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.reporter.type", defaultValue = "noop")
    private String reporter;

    @Inject
    @Documentation("When using url or kafka reporter, you can configure the async reporter with properties passed to this configuration entry."
            + "Ex: `messageTimeout=5000,closeTimeout=5000`.")
    @ConfigProperty(name = "talend.component.server.monitoring.brave.reporter.async", defaultValue = "console")
    private String reporterAsyncConfiguration;

    @Inject
    @Documentation("Should the /documentation endpoint be activated.")
    @ConfigProperty(name = "talend.component.server.documentation.active", defaultValue = "true")
    private Boolean supportsDocumentation;

    // sync with org.talend.sdk.component.server.service.security.SecurityExtension.addSecurityHandlers
    @Inject
    @Documentation("How to validate a connection. Accepted values: securityNoopHandler.")
    @ConfigProperty(name = "talend.component.server.security.connection.handler", defaultValue = "securityNoopHandler")
    private String securityConnectionHandler;

    // sync with org.talend.sdk.component.server.service.security.SecurityExtension.addSecurityHandlers(
    @Inject
    @Documentation("How to validate a command/request. Accepted values: securityNoopHandler.")
    @ConfigProperty(name = "talend.component.server.security.command.handler", defaultValue = "securityNoopHandler")
    private String securityCommandHandler;

    @Inject
    @Documentation("Should the component extensions add required dependencies.")
    @ConfigProperty(name = "talend.component.server.component.extend.dependencies", defaultValue = "true")
    private Boolean addExtensionDependencies;
}
