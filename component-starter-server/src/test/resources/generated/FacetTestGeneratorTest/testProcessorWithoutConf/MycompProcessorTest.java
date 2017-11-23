package foo.bar.processor;


import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.junit.JoinInputFactory;
import org.talend.sdk.component.junit.SimpleComponentRule;
import org.talend.sdk.component.runtime.output.Processor;

public class MycompProcessorTest {

    @ClassRule
    public static final SimpleComponentRule COMPONENT_FACTORY = new SimpleComponentRule("foo.bar");

    @Test
    @Ignore("You need to complete this test")
    public void map() throws IOException {

        // Processor configuration
        // Setup your component configuration for the test here
        final MycompProcessorConfiguration configuration =  new MycompProcessorConfiguration();

        // We create the component processor instance using the configuration filled above
        final Processor processor = COMPONENT_FACTORY.createProcessor(MycompProcessor.class, configuration);

        // The join input factory construct inputs test data for every input branch you have defined for this component
        // Make sure to fil in some test data for the branches you want to test
        // You can also remove the branches that you don't need from the factory below
        final JoinInputFactory joinInputFactory =  new JoinInputFactory()
                                                            .withInput("__default__", asList(/* TODO - list of your input data for this branch. Instances of ObjectMap.class */));


        // Run the flow and get the outputs
        final SimpleComponentRule.Outputs outputs = COMPONENT_FACTORY.collect(processor, joinInputFactory);

        // TODO - Test Asserts
        assertEquals(1, outputs.size()); // test of the output branches count of the component

        // Here you have all your processor output branches
        // You can fill in the expected data for every branch to test them
        final List<ObjectMap> value___default__ = outputs.get(ObjectMap.class, "__default__");
        assertEquals(asList(/* TODO - give a list of your expected values here. Instances of ObjectMap.class */), value___default__);

    }

}