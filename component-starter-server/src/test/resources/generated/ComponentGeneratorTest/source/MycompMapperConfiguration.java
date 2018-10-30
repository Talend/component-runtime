package com.foo.source;

import java.io.Serializable;

import com.foo.configuration.TestDataSet;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "dataset" }),
    @GridLayout.Row({ "name" })
})
@Documentation("TODO fill the documentation for this configuration")
public class MycompMapperConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private TestDataSet dataset;

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private String name;

    public TestDataSet getDataset() {
        return dataset;
    }

    public MycompMapperConfiguration setDataset(TestDataSet dataset) {
        this.dataset = dataset;
        return this;
    }

    public String getName() {
        return name;
    }

    public MycompMapperConfiguration setName(String name) {
        this.name = name;
        return this;
    }
}
