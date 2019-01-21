package com.foo.dataset;

import java.io.Serializable;

import com.foo.datastore.MyDataStore;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@DataSet("MyDataSet")
@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "theDatastore" })
})
@Documentation("TODO fill the documentation for this configuration")
public class MyDataSet implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private MyDataStore theDatastore;

    public MyDataStore getTheDatastore() {
        return theDatastore;
    }

    public MyDataSet setTheDatastore(MyDataStore theDatastore) {
        this.theDatastore = theDatastore;
        return this;
    }
}