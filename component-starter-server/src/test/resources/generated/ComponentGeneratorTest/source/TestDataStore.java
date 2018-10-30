package com.foo.configuration;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.meta.Documentation;

@DataStore("default")
@Documentation("TODO fill the documentation for this configuration")
public class TestDataStore implements Serializable {
    // fill the datastore/connection configuration
}
