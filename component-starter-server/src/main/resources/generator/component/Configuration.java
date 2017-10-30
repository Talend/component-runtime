package {{package}};

import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;

// generated configuration with query and addresses options, customize it to your need
@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed{{#structure}}
    @GridLayout.Row({ "{{name}}" }){{^-last}},{{/-last}}{{/structure}}
})
public class {{className}} {
    {{#structure}}
    @Option
    private {{type}} {{name}};{{^-last}}

{{/-last}}{{/structure}}
    {{#structure}}

    public {{type}} {{methodName}}() {
        return {{name}};
    }
{{/structure}}
}
