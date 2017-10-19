// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.configuration;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.Configuration;

// TODO: enhance documentation with that generated
@ApplicationScoped
@Configuration(prefix = "talend.component.server.")
public interface ComponentServerConfiguration {

    @ConfigProperty(name = "maven.repository", defaultValue = "${user.home}/.m2/repository")
    String mavenRepository();

    // property to list plugins directly by gav. This is nice to set it on the cli but not as a maintenance solution.
    @ConfigProperty(name = "component.coordinates", converter = ConfigurationConverters.SetConverter.class)
    Set<String> componentCoordinates();

    // property to list plugins like in a fatjar, ie value = gav. Nice for assemblies, less for demo/cli usage.
    @ConfigProperty(name = "component.registry")
    String componentRegistry();

    @ConfigProperty(name = "execution.pool.size", defaultValue = "64")
    int executionPoolSize();

    @ConfigProperty(name = "execution.pool.wait", defaultValue = "PT10S") // 10s
    String executionPoolShutdownTimeout();

    @ConfigProperty(name = "execution.dataset.retriver.timeout", defaultValue = "180") // in sec
    long datasetRetrieverTimeout();
}
