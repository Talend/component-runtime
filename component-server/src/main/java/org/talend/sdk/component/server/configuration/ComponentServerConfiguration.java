/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.Configuration;
import org.talend.sdk.component.api.meta.Documentation;

@ApplicationScoped
@Configuration(prefix = "talend.component.server.")
public interface ComponentServerConfiguration {

    @Documentation("If set it will replace any message for exceptions. Set to `false` to use the actual exception message.")
    @ConfigProperty(name = "jaxrs.exceptionhandler.defaultMessage", defaultValue = "false")
    String defaultExceptionMessage();

    @Documentation("The local maven repository used to locate components and their dependencies")
    @ConfigProperty(name = "maven.repository", defaultValue = "${user.home}/.m2/repository")
    String mavenRepository();

    // property to list plugins directly by gav. This is nice to set it on the cli but not as a maintenance solution.
    @Documentation("A comma separated list of gav to locate the components")
    @ConfigProperty(name = "component.coordinates", converter = ConfigurationConverters.SetConverter.class)
    Set<String> componentCoordinates();

    // property to list plugins like in a fatjar, ie value = gav. Nice for assemblies, less for demo/cli usage.
    @Documentation("A property file where the value is a gav of a component to register (complementary with `coordinates`)")
    @ConfigProperty(name = "component.registry")
    String componentRegistry();

    @Documentation("The size of the execution pool for runtime endpoints.")
    @ConfigProperty(name = "execution.pool.size", defaultValue = "64")
    int executionPoolSize();

    @Documentation("How long the application waits during shutdown for the execution tasks to complete")
    @ConfigProperty(name = "execution.pool.wait", defaultValue = "PT10S") // 10s
    String executionPoolShutdownTimeout();

    @Documentation("How long the read execution endpoint can last (max)")
    @ConfigProperty(name = "execution.dataset.retriever.timeout", defaultValue = "180") // in sec
    long datasetRetrieverTimeout();

    @Documentation("The name used by the brave integration (zipkin)")
    @ConfigProperty(name = "monitoring.brave.service.name", defaultValue = "component-server")
    String serviceName();

    @Documentation("The accuracy rate of the sampling.")
    @ConfigProperty(name = "monitoring.brave.sampling.rate", defaultValue = "-1.")
    float samplerRate();

    @Documentation("The accuracy rate of the sampling for environment endpoints.")
    @ConfigProperty(name = "monitoring.brave.sampling.environment.rate", defaultValue = "-1")
    float samplerEnvironmentRate();

    @Documentation("The accuracy rate of the sampling for environment endpoints.")
    @ConfigProperty(name = "monitoring.brave.sampling.configurationtype.rate", defaultValue = "-1")
    float samplerConfigurationTypeRate();

    @Documentation("The accuracy rate of the sampling for component endpoints.")
    @ConfigProperty(name = "monitoring.brave.sampling.component.rate", defaultValue = "-1")
    float samplerComponentRate();

    @Documentation("The accuracy rate of the sampling for action endpoints.")
    @ConfigProperty(name = "monitoring.brave.sampling.action.rate", defaultValue = "-1")
    float samplerActionRate();

    @Documentation("The accuracy rate of the sampling for execution endpoints.")
    @ConfigProperty(name = "monitoring.brave.sampling.execution.rate", defaultValue = "1")
    float samplerExecutionRate();

    @Documentation("The brave reporter to use to send the spans. Supported values are [auto, console, noop, url]. When configuration is needed,"
            + "you can use this syntax to configure the repoter if needed: `<name>(config1=value1, config2=value2)`, "
            + "for example: `url(endpoint=http://brave.company.com`.\n\n"
            + "In `auto` mode, if environment variable `TRACING_ON` doesn't exist or is set to `false`, `noop` will be selected, "
            + "and is set to `true`, `TRACING_KAFKA_URL`, `TRACING_KAFKA_TOPIC` and `TRACING_SAMPLING_RATE` will configure `kafka` reporter..")
    @ConfigProperty(name = "monitoring.brave.reporter.type", defaultValue = "auto")
    String reporter();

    @Documentation("When using url or kafka reporter, you can configure the async reporter with properties passed to this configuration entry."
            + "Ex: `messageTimeout=5000,closeTimeout=5000`.")
    @ConfigProperty(name = "monitoring.brave.reporter.type", defaultValue = "console")
    String reporterAsyncConfiguration();

    @Documentation("Should the /documentation endpoint be activated.")
    @ConfigProperty(name = "monitoring.documentation.active", defaultValue = "true")
    boolean supportsDocumentation();

    // sync with org.talend.sdk.component.server.service.security.SecurityExtension.addSecurityHandlers()
    @Documentation("How to validate a connection. Accepted values: securityNoopHandler.")
    @ConfigProperty(name = "security.connection.handler", defaultValue = "securityNoopHandler")
    String securityConnectionHandler();

    // sync with org.talend.sdk.component.server.service.security.SecurityExtension.addSecurityHandlers(
    @Documentation("How to validate a command/request. Accepted values: securityNoopHandler.")
    @ConfigProperty(name = "security.command.handler", defaultValue = "securityNoopHandler")
    String securityCommandHandler();
}
