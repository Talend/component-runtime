package com.foo.source;

import java.io.Serializable;

import com.foo.dataset.MyDataSet;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "identifier" }),
    @GridLayout.Row({ "theDataSet" })
})
@Documentation("TODO fill the documentation for this configuration")
public class MycompMapperConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String identifier;

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private MyDataSet theDataSet;

    public String getIdentifier() {
        return identifier;
    }

    public MycompMapperConfiguration setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public MyDataSet getTheDataSet() {
        return theDataSet;
    }

    public MycompMapperConfiguration setTheDataSet(MyDataSet theDataSet) {
        this.theDataSet = theDataSet;
        return this;
    }
}