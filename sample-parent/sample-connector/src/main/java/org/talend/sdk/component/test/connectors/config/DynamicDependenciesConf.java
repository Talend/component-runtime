package org.talend.sdk.component.test.connectors.config;


import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DynamicDependenciesServiceConfiguration;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@DynamicDependenciesServiceConfiguration
@GridLayout({
        @GridLayout.Row({"group"}),
        @GridLayout.Row({"artifact"})
})
public class DynamicDependenciesConf implements Serializable {

    @Option
    @Documentation("The maven group.")
    private String group;

    @Option
    @Documentation("The maven artifact id.")
    private String artifact;

}