package com.foo.output;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "username" }),
    @GridLayout.Row({ "password" })
})
@Documentation("TODO fill the documentation for this configuration")
public class CredentialConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String username;

    @Credential
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String password;

    public String getUsername() {
        return username;
    }

    public CredentialConfiguration setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public CredentialConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }
}