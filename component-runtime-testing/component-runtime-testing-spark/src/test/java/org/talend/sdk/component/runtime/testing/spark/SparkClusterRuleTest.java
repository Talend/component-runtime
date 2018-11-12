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
 * WITHOUT WARRANTIES OR CONhttps://svn.apache.org/repos/asf/geronimo/xbean/tags/xbean-4.6/DITIONS OF ANY KIND, either
 * express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.testing.spark;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;

import scala.Tuple2;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import lombok.NoArgsConstructor;

public class SparkClusterRuleTest {

    @ClassRule
    public static final SparkClusterRule SPARK = new SparkClusterRule("2.11", "2.4.0", 1);

    @Rule
    public final TestName testName = new TestName();

    @Test
    public void classpathSubmit() {
        final File out =
                new File(jarLocation(SparkClusterRuleTest.class).getParentFile(), testName.getMethodName() + ".out");
        if (out.exists()) {
            out.delete();
        }
        SPARK.submitClasspath(SubmittableMain.class, File::isDirectory, SPARK.getSparkMaster(), out.getAbsolutePath());

        await()
                .atMost(5, MINUTES)
                .until(() -> out.exists() ? Files.readAllLines(out.toPath()).stream().collect(joining("\n")).trim()
                        : null, equalTo("b -> 1\na -> 1"));
    }

    @NoArgsConstructor(access = PRIVATE)
    public static class SubmittableMain {

        public static void main(final String[] args) {
            final SparkConf conf = new SparkConf().setAppName(SubmittableMain.class.getName()).setMaster(args[0]);
            final JavaSparkContext context = new JavaSparkContext(conf);

            context
                    .parallelize(singletonList("a b"))
                    .flatMap((FlatMapFunction<String, String>) text -> asList(text.split(" ")).iterator())
                    .mapToPair(word -> new Tuple2<>(word, 1))
                    .reduceByKey((a, b) -> a + b)
                    .foreach(result -> {
                        try (final FileWriter writer = new FileWriter(args[1], true)) {
                            writer.write(result._1 + " -> " + result._2 + '\n');
                        }
                    });
        }
    }
}
