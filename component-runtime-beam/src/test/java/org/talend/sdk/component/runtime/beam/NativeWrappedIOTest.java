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
package org.talend.sdk.component.runtime.beam;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.json.JsonObject;

import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.junit.SimpleComponentRule;
import org.talend.sdk.component.junit.SimpleFactory;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.test.wrapped.JdbcSource;

public class NativeWrappedIOTest {

    @ClassRule
    public static final SimpleComponentRule COMPONENTS =
            new SimpleComponentRule(JdbcSource.class.getPackage().getName())
                    .withIsolatedPackage(JdbcSource.class.getPackage().getName(), JdbcIO.class.getPackage().getName());

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    public void dofn() {
        final JdbcSource.Config config = new JdbcSource.Config();
        config.setDriver("org.hsqldb.jdbcDriver");
        config.setUrl("jdbc:hsqldb:mem:foo");
        config.setUsername("sa");
        config.setPassword("");
        config.setQuery("SELECT * FROM   INFORMATION_SCHEMA.TABLES");
        final Map<String, String> map = SimpleFactory.configurationByExample().forInstance(config).configured().toMap();
        final String plugin = COMPONENTS.getTestPlugins().iterator().next();
        final PTransform<PBegin, PCollection<JsonObject>> jdbc = PTransform.class.cast(COMPONENTS
                .asManager()
                .createComponent("Jdbc", "Input", ComponentManager.ComponentType.MAPPER, 1, map)
                .orElseThrow(() -> new IllegalArgumentException("no jdbc input")));
        PAssert.that(pipeline.apply(jdbc).setCoder(JsonpJsonObjectCoder.of(plugin))).satisfies(
                (SerializableFunction<Iterable<JsonObject>, Void>) input -> {
                    final JsonObject next = input.iterator().next();
                    assertEquals("PUBLIC", next.getString("TABLE_CATALOG"));
                    return null;
                });
        pipeline.run().waitUntilFinish();
    }

    @Test
    public void source() {
        final String plugin = COMPONENTS.getTestPlugins().iterator().next();
        final PTransform<PBegin, PCollection<JsonObject>> jdbc = PTransform.class.cast(COMPONENTS
                .asManager()
                .createComponent("beamtest", "source", ComponentManager.ComponentType.MAPPER, 1, emptyMap())
                .orElseThrow(() -> new IllegalArgumentException("no beamtest#source component")));
        PAssert.that(pipeline.apply(jdbc).setCoder(JsonpJsonObjectCoder.of(plugin))).satisfies(
                (SerializableFunction<Iterable<JsonObject>, Void>) input -> {
                    assertEquals("test", input.iterator().next().getString("id"));
                    return null;
                });
        pipeline.run().waitUntilFinish();
    }
}
