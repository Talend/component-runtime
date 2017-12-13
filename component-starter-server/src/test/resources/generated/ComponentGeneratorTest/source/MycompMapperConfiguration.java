package com.foo.source;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "name" })
})
@Documentation("TODO fill the documentation for this configuration")
public class MycompMapperConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String name;

    public String getName() {
        return name;
    }

    public MycompMapperConfiguration setName(String name) {
        this.name = name;
        return this;
    }
}