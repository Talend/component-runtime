package com.foo.datastore;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@DataStore("MyDataStore")
@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "person" })
})
@Documentation("TODO fill the documentation for this configuration")
public class MyDataStore implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private MyDataStorePersonConfiguration person;

    public MyDataStorePersonConfiguration getPerson() {
        return person;
    }

    public MyDataStore setPerson(MyDataStorePersonConfiguration person) {
        this.person = person;
        return this;
    }
}