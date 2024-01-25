/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.test.wrapped;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.util.common.ElementByteSizeObserver;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;
import org.talend.sdk.component.api.input.PartitionMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;

@Icon(Icon.IconType.DB_INPUT)
@PartitionMapper(family = "Jdbc", name = "Input")
public class JdbcSource extends PTransform<PBegin, PCollection<JsonObject>> {

    private final Config config;

    private final JsonBuilderFactory builder;

    public JdbcSource(@Option("configuration") final Config config, final JsonBuilderFactory builder) {
        this.config = config;
        this.builder = builder;
    }

    public PCollection<JsonObject> expand(final PBegin input) {
        final WorkAroundCoder workAroundCoder = new WorkAroundCoder();
        final PCollection<JsonObject> apply = input
                .apply(JdbcIO
                        .<JsonObject> read()
                        .withRowMapper(new RecordMapper(builder))
                        .withDataSourceConfiguration(config.asBeamConfig())
                        .withQuery(config.query)
                        .withCoder(workAroundCoder));
        workAroundCoder.collection = apply;
        return apply;
    }

    // the io shouldnt require a coder since user can set it on the pipeline
    public static class WorkAroundCoder extends Coder<JsonObject> {

        private static final long serialVersionUID = 1L;

        private transient PCollection<JsonObject> collection;

        private Coder<JsonObject> delegate;

        private Coder<JsonObject> delegate() {
            return delegate == null ? delegate = JsonpJsonObjectCoder.of("test-classes") : delegate;
        }

        @Override
        public void encode(final JsonObject value, final OutputStream outStream) throws IOException {
            delegate().encode(value, outStream);
        }

        @Override
        @Deprecated
        @Experimental(Experimental.Kind.CODER_CONTEXT)
        public void encode(final JsonObject value, final OutputStream outStream, final Context context)
                throws IOException {
            delegate().encode(value, outStream, context);
        }

        @Override
        public JsonObject decode(final InputStream inStream) throws IOException {
            return delegate().decode(inStream);
        }

        @Override
        @Deprecated
        @Experimental(Experimental.Kind.CODER_CONTEXT)
        public JsonObject decode(final InputStream inStream, final Context context) throws IOException {
            return delegate().decode(inStream, context);
        }

        @Override
        public List<? extends Coder<?>> getCoderArguments() {
            return delegate().getCoderArguments();
        }

        @Override
        public void verifyDeterministic() throws NonDeterministicException {
            delegate().verifyDeterministic();
        }

        @Override
        public boolean consistentWithEquals() {
            return delegate().consistentWithEquals();
        }

        @Override
        public Object structuralValue(final JsonObject value) {
            return delegate().structuralValue(value);
        }

        @Override
        public boolean isRegisterByteSizeObserverCheap(final JsonObject value) {
            return delegate().isRegisterByteSizeObserverCheap(value);
        }

        @Override
        public void registerByteSizeObserver(final JsonObject value, final ElementByteSizeObserver observer)
                throws Exception {
            delegate().registerByteSizeObserver(value, observer);
        }

        @Override
        @Experimental(Experimental.Kind.CODER_TYPE_ENCODING)
        public TypeDescriptor<JsonObject> getEncodedTypeDescriptor() {
            return delegate().getEncodedTypeDescriptor();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return Objects.equals(delegate(), WorkAroundCoder.class.cast(o).delegate());
        }

        @Override
        public int hashCode() {
            return WorkAroundCoder.class.hashCode();
        }

        Object writeReplace() throws ObjectStreamException {
            if (delegate == null) {
                delegate(); // force it to be init before the serialization
            }
            return this;
        }
    }

    @Data
    @GridLayout({ @GridLayout.Row("driver"), @GridLayout.Row("url"), @GridLayout.Row({ "username", "password" }),
            @GridLayout.Row("query") })
    public static class Config {

        @Option
        private String driver;

        @Option
        private String url;

        @Option
        private String username;

        @Option
        @Credential
        private String password;

        @Option
        private String query;

        JdbcIO.DataSourceConfiguration asBeamConfig() {
            final JdbcIO.DataSourceConfiguration configuration = JdbcIO.DataSourceConfiguration.create(driver, url);
            if (username != null) {
                return configuration.withUsername(username).withPassword(password);
            }
            return configuration;
        }
    }

    @RequiredArgsConstructor
    public static class RecordMapper implements JdbcIO.RowMapper<JsonObject> {

        private final JsonBuilderFactory builder;

        private volatile BiConsumer<JsonObjectBuilder, ResultSet>[] builders;

        @Override
        public JsonObject mapRow(final ResultSet resultSet) throws Exception {
            if (builders == null) {
                final ResultSetMetaData metaData = resultSet.getMetaData();
                final int count = metaData.getColumnCount();
                builders = IntStream.rangeClosed(1, count).mapToObj(i -> {
                    final String column;
                    try {
                        column = metaData
                                .getColumnName(

                                        i);
                    } catch (final SQLException e) {
                        throw new IllegalStateException(e);
                    }
                    return (BiConsumer<JsonObjectBuilder, ResultSet>) (b, r) -> {
                        final Object object;
                        try {
                            object = r.getObject(i);
                        } catch (final SQLException e) {
                            throw new IllegalStateException(e);
                        }
                        if (object == null) {
                            b.addNull(column);
                        } else {
                            b.add(column, String.valueOf(object)); // for demo purposes
                        }
                    };
                }).toArray(BiConsumer[]::new);
            }

            final JsonObjectBuilder current = builder.createObjectBuilder();
            Stream.of(builders).forEach(b -> b.accept(current, resultSet));
            return current.build();
        }
    }
}
