package com.foo.output;

import java.io.Serializable;

import com.foo.configuration.TestDataSet;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "dataset" }),
    @GridLayout.Row({ "host" }),
    @GridLayout.Row({ "port" }),
    @GridLayout.Row({ "credential" })
})
@Documentation("TODO fill the documentation for this configuration")
public class TProcOutputConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private TestDataSet dataset;

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String host;

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String port;

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private CredentialConfiguration credential;

    public TestDataSet getDataset() {
        return dataset;
    }

    public TProcOutputConfiguration setDataset(TestDataSet dataset) {
        this.dataset = dataset;
        return this;
    }

    public String getHost() {
        return host;
    }

    public TProcOutputConfiguration setHost(String host) {
        this.host = host;
        return this;
    }

    public String getPort() {
        return port;
    }

    public TProcOutputConfiguration setPort(String port) {
        this.port = port;
        return this;
    }

    public CredentialConfiguration getCredential() {
        return credential;
    }

    public TProcOutputConfiguration setCredential(CredentialConfiguration credential) {
        this.credential = credential;
        return this;
    }
}
