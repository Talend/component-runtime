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
package org.talend.sdk.component.server.configuration;

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
    private Optional<String> componentCoordinates;

    // property to list plugins like in a fatjar, ie value = gav. Nice for assemblies, less for demo/cli usage.
    @Inject
    @Documentation("A property file where the value is a gav of a component to register (complementary with `coordinates`)")
    @ConfigProperty(name = "talend.component.server.component.registry")
    private Optional<String> componentRegistry;

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

    @Inject
    @Documentation("A component translation repository. This is where you put your documentation translations. "
            + "Their name must follow the pattern `documentation_${container-id}_language.adoc` where `${container-id}` "
            + "is the component jar name (without the extension and version, generally the artifactId).")
    @ConfigProperty(name = "talend.component.server.component.documentation.translations",
            defaultValue = "${home}/documentations")
    private String documentationI18nTranslations;
}
