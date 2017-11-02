/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.server.configuration;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.Configuration;
import org.talend.sdk.component.api.meta.Documentation;

// TODO: enhance documentation with that generated
@ApplicationScoped
@Configuration(prefix = "talend.component.server.")
public interface ComponentServerConfiguration {
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
}
