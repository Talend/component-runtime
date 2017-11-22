package com.foo.source;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "person" })
})
public class MycompSourceConfiguration {

    @Option
    private PersonConfiguration person;

    public PersonConfiguration getPerson() {
        return person;
    }

    public MycompSourceConfiguration  setPerson(PersonConfiguration person) {
         this.person = person;
         return this;
    }

}