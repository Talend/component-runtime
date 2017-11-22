package com.foo.source;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "name" })
})
public class MycompSourceConfiguration {

    @Option
    private String name;

    public String getName() {
        return name;
    }

    public MycompSourceConfiguration  setName(String name) {
         this.name = name;
         return this;
    }

}