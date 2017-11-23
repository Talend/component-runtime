package com.foo.processor;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.ListCoder;
import org.apache.beam.sdk.coders.MapCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.junit.JoinInputFactory;
import org.talend.sdk.component.junit.RecordAsserts;
import org.talend.sdk.component.junit.SimpleComponentRule;
import org.talend.sdk.component.runtime.beam.TalendCoder;
import org.talend.sdk.component.runtime.beam.TalendFn;
import org.talend.sdk.component.runtime.output.Processor;

public class TInProcessorBeamTest {
    @ClassRule
    public static final SimpleComponentRule COMPONENT_FACTORY = new SimpleComponentRule("com.foo");

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    @Ignore("You need to complete this test with your own data and assertions")
    public void processor() {
        // Processor configuration
        // Setup your component configuration for the test here
        final TInProcessorConfiguration configuration =  new TInProcessorConfiguration();

        // We create the component processor instance using the configuration filled above
        final Processor processor = COMPONENT_FACTORY.createProcessor(TInProcessor.class, configuration);

        // The join input factory construct inputs test data for every input branch you have defined for this component
        // Make sure to fil in some test data for the branches you want to test
        // You can also remove the branches that you don't need from the factory below
        final JoinInputFactory joinInputFactory =  new JoinInputFactory()
                .withInput("__default__", asList(/* TODO - list of your input data for this branch. Instances of ObjectMap.class */));

        // Convert it to a beam "source"
        final PCollection<Map<String, List<Serializable>>> inputs = pipeline.apply(
                Create.of(joinInputFactory.asInputRecords())
                    .withCoder(MapCoder.of(StringUtf8Coder.of(), ListCoder.of(TalendCoder.of()))));

        // add our processor right after to see each data as configured previously
        final PCollection<Map<String, List<Serializable>>> result = inputs.apply(TalendFn.asFn(processor));

        PAssert.that(result)
            .satisfies(new RecordAsserts()
                .withAsserts("__default__", dataList -> {
                    // assert resulting data of the branch "__default__", for instance:
                    // assertEquals(10, dataList.size());
                })
                ::apply);

        // run the pipeline and ensure the execution was successful
        assertEquals(PipelineResult.State.DONE, pipeline.run().getState());
    }
}