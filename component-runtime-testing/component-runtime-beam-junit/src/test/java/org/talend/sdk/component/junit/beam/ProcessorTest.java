package org.talend.sdk.component.junit.beam;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.junit.JoinInputFactory;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.output.ProcessorImpl;

import lombok.AllArgsConstructor;
import lombok.Data;

@Ignore
public class ProcessorTest {

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    @Ignore("You need to complete this test with your own data and assertions")
    public void processor() {

        final Processor processor = new ProcessorImpl("root", "processor", "test", new SampleProcessor());

        // The join input factory construct inputs test data for every input branch you have defined for this component
        // Make sure to fil in some test data for the branches you want to test
        // You can also remove the branches that you don't need from the factory below
        final JoinInputFactory joinInputFactory = new JoinInputFactory()
                .withInput("__default__",
                        asList(/* TODO - list of your input data for this branch. Instances of CompanyComponent4DefaultInput.class */));

        // Convert it to a beam "source"
        //fixme 
        //        final MapCoder<String, List<Serializable>> coder = MapCoder.of(StringUtf8Coder.of(),
        //                ListCoder.of(SerializableCoder.of(Serializable.class)));
        //        final PCollection<Map<String, List<Serializable>>> inputs = pipeline.apply(
        //                Create.of(joinInputFactory.asInputRecords())
        //                        .withCoder(coder));

        // add our processor right after to see each data as configured previously
        //todo inputs.apply(TalendFn.asFn(processor));

        // run the pipeline and ensure the execution was successful
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    public static class SampleProcessor implements Serializable {

        @ElementListener
        public Sample onNext(final Sample sample) {
            stack.add("next{" + sample.data + "}");
            return sample;
        }

        final Collection<String> stack = new ArrayList<>();

        @PostConstruct
        public void init() {
            stack.add("start");
        }

        @BeforeGroup
        public void beforeGroup() {
            stack.add("beforeGroup");
        }

        @AfterGroup
        public void afterGroup() {
            stack.add("afterGroup");
        }

        @PreDestroy
        public void destroy() {
            stack.add("stop");
        }
    }

    @Data
    @AllArgsConstructor
    public static class Sample {

        private int data;
    }

}
