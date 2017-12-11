package com.foo.source;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "name" }),
    @GridLayout.Row({ "age" })
})
@Documentation("TODO fill the documentation for this configuration")
public class PersonConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String name;

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private int age;

    public String getName() {
        return name;
    }

    public PersonConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public int getAge() {
        return age;
    }

    public PersonConfiguration setAge(int age) {
        this.age = age;
        return this;
    }
}