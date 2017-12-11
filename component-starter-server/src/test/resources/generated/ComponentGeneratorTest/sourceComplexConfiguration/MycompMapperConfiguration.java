package com.foo.source;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "person" })
})
@Documentation("TODO fill the documentation for this configuration")
public class MycompMapperConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private PersonConfiguration person;

    public PersonConfiguration getPerson() {
        return person;
    }

    public MycompMapperConfiguration setPerson(PersonConfiguration person) {
        this.person = person;
        return this;
    }
}