package com.foo.output;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.values.PCollection;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.junit.JoinInputFactory;
import org.talend.sdk.component.junit.SimpleComponentRule;
import org.talend.sdk.component.junit.beam.Data;
import org.talend.sdk.component.runtime.beam.TalendFn;
import org.talend.sdk.component.runtime.output.Processor;

public class TInOutputBeamTest {
    @ClassRule
    public static final SimpleComponentRule COMPONENT_FACTORY = new SimpleComponentRule("com.foo");

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    @Ignore("You need to complete this test with your own data and assertions")
    public void processor() {
        // Output configuration
        // Setup your component configuration for the test here
        final TInOutputConfiguration configuration =  new TInOutputConfiguration();

        // We create the component processor instance using the configuration filled above
        final Processor processor = COMPONENT_FACTORY.createProcessor(TInOutput.class, configuration);

        // The join input factory construct inputs test data for every input branch you have defined for this component
        // Make sure to fil in some test data for the branches you want to test
        // You can also remove the branches that you don't need from the factory below
        final JoinInputFactory joinInputFactory =  new JoinInputFactory()
                .withInput("__default__", asList(/* TODO - list of your input data for this branch. Instances of Record.class */));

        // Convert it to a beam "source"
        final PCollection<Record> inputs =
                pipeline.apply(Data.of(processor.plugin(), joinInputFactory.asInputRecords()));

        // add our processor right after to see each data as configured previously
        inputs.apply(TalendFn.asFn(processor))
                .apply(Data.map(processor.plugin(), Record.class));

        // run the pipeline and ensure the execution was successful
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}