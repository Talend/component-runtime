/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.talend.sdk.component.runtime.beam;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BeamPipelineTest {

    @Rule
    public final TemporaryFolder TEMP_FOLDER = new TemporaryFolder();

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    private static final String INPUT_NAME = "input.csv";

    private static final String OUTPUT_NAME = "output.csv";

    private static final String ADDITIONAL_SUFFIX = ";";

    private static final List<String> TEST_VALUES = Arrays.asList("a", "b", "c");

    @Before
    public void prepare() throws IOException {
        File input = TEMP_FOLDER.newFile(INPUT_NAME);
        try (PrintStream writer = new PrintStream(new FileOutputStream(input))) {
            TEST_VALUES.forEach(s -> writer.println(s));
        }
    }

    @Test
    public void testComponentWithSparkRunner() throws IOException {
        ComponentManager manager = ComponentManager.instance();

        pipeline.apply(TalendIO.read(manager.findMapper("test", "reader", 1, new HashMap<String, String>() {

            {
                put("file", new File(TEMP_FOLDER.getRoot(), INPUT_NAME).getAbsolutePath());
            }
        }).get()))
                .apply(TalendFn.asFn(manager.findProcessor("test", "processor", 1, emptyMap()).get(), emptyMap()))
                .apply(TalendIO.write(manager.findProcessor("test", "writer", 1, new HashMap<String, String>() {

                    {
                        put("file", new File(TEMP_FOLDER.getRoot(), OUTPUT_NAME).getAbsolutePath());
                    }
                }).get(), emptyMap()));
        pipeline.run();
        final PipelineResult.State state = pipeline.run().waitUntilFinish();
        assertFile(new File(TEMP_FOLDER.getRoot(), OUTPUT_NAME));
    }

    private void assertFile(File file) throws IOException {
        assertTrue("Output file doesn't exist", file.exists());
        assertEquals("TEST_VALUES collected are not equal to File content",
                TEST_VALUES.stream().map(s -> s.concat(ADDITIONAL_SUFFIX)).collect(Collectors.joining("\n")),
                Files.lines(file.toPath()).collect(joining("\n")));
    }

    @Emitter(family = "test", name = "reader")
    public static class SimpleReader implements Serializable {

        private final File file;

        private transient BufferedReader bReader;

        public SimpleReader(@Option("file") File file) {
            this.file = file;
        }

        @PostConstruct
        public void open() throws FileNotFoundException {
            bReader = new BufferedReader(new FileReader(file));
        }

        @Producer
        public String readNext() throws IOException {
            final String line = bReader.readLine();
            if (line == null) { // end of the data is marked with null
                return null;
            }
            return line;
        }

        @PreDestroy
        public void close() throws IOException {
            if (bReader != null) {
                bReader.close();
            }
        }

    }

    @Processor(family = "test", name = "processor")
    public static class SimpleProcessor implements Serializable {

        @ElementListener
        public String process(final String line) throws IOException {
            return line + ADDITIONAL_SUFFIX;
        }

    }

    @Processor(family = "test", name = "writer")
    public static class SimpleWriter implements Serializable {

        private final File file;

        private transient PrintStream writer;

        public SimpleWriter(@Option("file") File file) {
            this.file = file;
        }

        @PostConstruct
        public void open() throws IOException {
            writer = new PrintStream(new FileOutputStream(file));
        }

        @PreDestroy
        public void close() throws IOException {
            if (writer != null) {
                writer.close();
            }
        }

        @ElementListener
        public void write(String line) {
            writer.println(line);
        }

    }

}
