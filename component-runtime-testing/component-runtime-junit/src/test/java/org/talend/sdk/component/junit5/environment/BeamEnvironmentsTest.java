/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.junit5.environment;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterAll;
import org.talend.sdk.component.junit.environment.Environment;
import org.talend.sdk.component.junit.environment.EnvironmentConfiguration;
import org.talend.sdk.component.junit.environment.builtin.beam.DirectRunnerEnvironment;
import org.talend.sdk.component.junit.environment.builtin.beam.FlinkRunnerEnvironment;
import org.talend.sdk.component.junit.environment.builtin.beam.SparkRunnerEnvironment;

@Environment(DirectRunnerEnvironment.class)
@EnvironmentConfiguration(environment = "Direct",
        systemProperties = @EnvironmentConfiguration.Property(key = "BeamEnvironmentsTest", value = "direct-test"))

@Environment(SparkRunnerEnvironment.class)
@EnvironmentConfiguration(environment = "Spark",
        systemProperties = @EnvironmentConfiguration.Property(key = "BeamEnvironmentsTest", value = "spark-test"))

@Environment(FlinkRunnerEnvironment.class)
@EnvironmentConfiguration(environment = "Flink",
        systemProperties = @EnvironmentConfiguration.Property(key = "BeamEnvironmentsTest", value = "flink-test"))
class BeamEnvironmentsTest {

    private static final Collection<String> EXECUTIONS = new ArrayList<>();

    @EnvironmentalTest
    void execute() throws ClassNotFoundException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final String runner = ServiceLoader
                .load(classLoader.loadClass("org.apache.beam.sdk.runners.PipelineRunnerRegistrar"))
                .iterator()
                .next()
                .getClass()
                .getName();
        EXECUTIONS.add(System.getProperty("BeamEnvironmentsTest") + "/" + runner);
    }

    @AfterAll
    static void asserts() {
        assertEquals(asList("direct-test/org.apache.beam.runners.direct.DirectRegistrar$Runner",
                "spark-test/org.apache.beam.runners.spark.SparkRunnerRegistrar$Runner",
                "flink-test/org.apache.beam.runners.flink.FlinkRunnerRegistrar$Runner"), EXECUTIONS);
    }
}
