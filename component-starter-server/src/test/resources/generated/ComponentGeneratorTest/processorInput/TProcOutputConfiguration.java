package com.foo.output;

import java.io.Serializable;

import com.foo.configuration.TestDataSet;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
    // the generated layout put one configuration entry per line,
    // customize it as much as needed
    @GridLayout.Row({ "dataset" })
})
@Documentation("TODO fill the documentation for this configuration")
public class TProcOutputConfiguration implements Serializable {
    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private TestDataSet dataset;

    public TestDataSet getDataset() {
        return dataset;
    }

    public TProcOutputConfiguration setDataset(TestDataSet dataset) {
        this.dataset = dataset;
        return this;
    }
}
