package com.foo.output;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "host" }),
    @GridLayout.Row({ "port" }),
    @GridLayout.Row({ "credential" })
})
public class TProcOutputConfiguration implements Serializable {
    @Option
    private String host;

    @Option
    private String port;

    @Option
    private CredentialConfiguration credential;

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