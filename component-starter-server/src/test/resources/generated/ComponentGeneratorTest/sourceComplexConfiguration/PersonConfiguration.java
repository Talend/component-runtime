package com.foo.source;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "name" }),
    @GridLayout.Row({ "age" })
})
public class PersonConfiguration {
    @Option
    private String name;

    @Option
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