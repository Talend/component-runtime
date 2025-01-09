/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.factory.service.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;

// dev note: keep the complexity of this class
// - ambiguous methods from just their name
// - complex parameters
// etc...
//
// this is all part of the test coverage we want
public class CustomJdbcIO {

    public static <T> Read<T> read() {
        return new AutoValue_CustomJdbcIO_Read.Builder<T>().build();
    }

    private CustomJdbcIO() {
        throw new AssertionError();
    }

    @FunctionalInterface
    public interface RowMapper<T> extends Serializable {

        T mapRow(ResultSet resultSet) throws Exception;
    }

    public abstract static class DataSourceConfiguration implements Serializable {

        abstract String getDriverClassName();

        abstract String getUrl();

        abstract String getUsername();

        abstract String getPassword();

        abstract String getConnectionProperties();

        abstract DataSource getDataSource();

        abstract DataSourceConfiguration.Builder builder();

        abstract static class Builder {

            abstract DataSourceConfiguration.Builder setDriverClassName(String driverClassName);

            abstract DataSourceConfiguration.Builder setUrl(String url);

            abstract DataSourceConfiguration.Builder setUsername(String username);

            abstract DataSourceConfiguration.Builder setPassword(String password);

            abstract DataSourceConfiguration.Builder setConnectionProperties(String connectionProperties);

            abstract DataSourceConfiguration.Builder setDataSource(DataSource dataSource);

            abstract DataSourceConfiguration build();
        }

        public static DataSourceConfiguration create(final DataSource dataSource) {
            checkArgument(dataSource != null, "dataSource can not be null");
            checkArgument(dataSource instanceof Serializable, "dataSource must be Serializable");
            return new AutoValue_CustomJdbcIO_DataSourceConfiguration.Builder().setDataSource(dataSource).build();
        }

        public static DataSourceConfiguration create(final String driverClassName, final String url) {
            checkArgument(driverClassName != null, "driverClassName can not be null");
            checkArgument(url != null, "url can not be null");
            return new AutoValue_CustomJdbcIO_DataSourceConfiguration.Builder()
                    .setDriverClassName(driverClassName)
                    .setUrl(url)
                    .build();
        }

        public DataSourceConfiguration withUsername(final String username) {
            return builder().setUsername(username).build();
        }

        public DataSourceConfiguration withPassword(final String password) {
            return builder().setPassword(password).build();
        }

        public DataSourceConfiguration withConnectionProperties(final String connectionProperties) {
            checkArgument(connectionProperties != null, "connectionProperties can not be null");
            return builder().setConnectionProperties(connectionProperties).build();
        }
    }

    public abstract static class Read<T> extends PTransform<PBegin, PCollection<T>> {

        abstract DataSourceConfiguration getDataSourceConfiguration();

        abstract ValueProvider<String> getQuery();

        abstract RowMapper<T> getRowMapper();

        abstract Read.Builder<T> toBuilder();

        abstract static class Builder<T> {

            abstract Read.Builder<T> setDataSourceConfiguration(DataSourceConfiguration config);

            abstract Read.Builder<T> setQuery(ValueProvider<String> query);

            abstract Read.Builder<T> setRowMapper(RowMapper<T> rowMapper);

            abstract Read<T> build();
        }

        public Read<T> withDataSourceConfiguration(final DataSourceConfiguration configuration) {
            checkArgument(configuration != null, "configuration can not be null");
            return toBuilder().setDataSourceConfiguration(configuration).build();
        }

        public Read<T> withQuery(final ValueProvider<String> query) {
            checkArgument(query != null, "query can not be null");
            return toBuilder().setQuery(query).build();
        }

        public Read<T> withQuery(final String query) {
            checkArgument(query != null, "query can not be null");
            return withQuery(ValueProvider.StaticValueProvider.of(query));
        }

        public Read<T> withRowMapper(final RowMapper<T> rowMapper) {
            checkArgument(rowMapper != null, "rowMapper can not be null");
            return toBuilder().setRowMapper(rowMapper).build();
        }

        @Override
        public PCollection<T> expand(final PBegin input) {
            throw new UnsupportedOperationException("not used in tests");
        }
    }
}
