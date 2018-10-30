package com.foo.configuration;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.meta.Documentation;

@DataSet("default")
@Documentation("TODO fill the documentation for this configuration")
public class TestDataSet implements Serializable {

    //
    // fill the reusable configuration for input/output components
    // -> it must also enable to instantiate a source component without additional *required* configuration
    // -> any input/output components must have a reference to a dataset to be valid for Talend Platform (cloud)
    //

    @Option
    @Documentation("The connection of the configuration")
    private TestDataStore connection;

    public TestDataStore getConnection() {
        return connection;
    }

    public TestDataSet setConnection(TestDataStore connection) {
        this.connection = connection;
        return this;
    }
}
