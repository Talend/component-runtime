package {{package}};

import java.util.List;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.ui.OptionsOrder;

// generated configuration with query and addresses options, customize it to your need
@OptionsOrder({ "query", "addresses" })
public class {{className}} {
    @Option
    private String query;

    @Option
    private List<String> addresses;

    public String getQuery() {
        return query;
    }

    public List<String> getAddresses() {
        return addresses;
    }
}
