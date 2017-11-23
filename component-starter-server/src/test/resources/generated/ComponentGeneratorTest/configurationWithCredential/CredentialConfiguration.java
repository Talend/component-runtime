package com.foo.output;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "username" }),
    @GridLayout.Row({ "password" })
})
public class CredentialConfiguration implements Serializable {
    @Option
    private String username;

    @Credential
    @Option
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