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

import javax.annotation.Generated;

import org.apache.beam.sdk.options.ValueProvider;

@Generated("from_com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_CustomJdbcIO_Read<T> extends CustomJdbcIO.Read<T> {

    private final CustomJdbcIO.DataSourceConfiguration dataSourceConfiguration;

    private final ValueProvider<String> query;

    private final CustomJdbcIO.RowMapper<T> rowMapper;

    private AutoValue_CustomJdbcIO_Read(final CustomJdbcIO.DataSourceConfiguration dataSourceConfiguration,
            final ValueProvider<String> query, final CustomJdbcIO.RowMapper<T> rowMapper) {
        this.dataSourceConfiguration = dataSourceConfiguration;
        this.query = query;
        this.rowMapper = rowMapper;
    }

    @Override
    CustomJdbcIO.DataSourceConfiguration getDataSourceConfiguration() {
        return dataSourceConfiguration;
    }

    @Override
    ValueProvider<String> getQuery() {
        return query;
    }

    @Override
    CustomJdbcIO.RowMapper<T> getRowMapper() {
        return rowMapper;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CustomJdbcIO.Read) {
            CustomJdbcIO.Read<?> that = (CustomJdbcIO.Read<?>) o;
            return ((this.dataSourceConfiguration == null) ? (that.getDataSourceConfiguration() == null)
                    : this.dataSourceConfiguration.equals(that.getDataSourceConfiguration()))
                    && ((this.query == null) ? (that.getQuery() == null) : this.query.equals(that.getQuery()))
                    && ((this.rowMapper == null) ? (that.getRowMapper() == null)
                            : this.rowMapper.equals(that.getRowMapper()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (dataSourceConfiguration == null) ? 0 : this.dataSourceConfiguration.hashCode();
        h *= 1000003;
        h ^= (query == null) ? 0 : this.query.hashCode();
        h *= 1000003;
        h ^= (rowMapper == null) ? 0 : this.rowMapper.hashCode();
        return h;
    }

    @Override
    CustomJdbcIO.Read.Builder<T> toBuilder() {
        return new Builder<T>(this);
    }

    static final class Builder<T> extends CustomJdbcIO.Read.Builder<T> {

        private CustomJdbcIO.DataSourceConfiguration dataSourceConfiguration;

        private ValueProvider<String> query;

        private CustomJdbcIO.RowMapper<T> rowMapper;

        Builder() {
            // no-op
        }

        private Builder(final CustomJdbcIO.Read<T> source) {
            this.dataSourceConfiguration = source.getDataSourceConfiguration();
            this.query = source.getQuery();
            this.rowMapper = source.getRowMapper();
        }

        @Override
        CustomJdbcIO.Read.Builder<T>
                setDataSourceConfiguration(final CustomJdbcIO.DataSourceConfiguration dataSourceConfiguration) {
            this.dataSourceConfiguration = dataSourceConfiguration;
            return this;
        }

        @Override
        CustomJdbcIO.Read.Builder<T> setQuery(final ValueProvider<String> query) {
            this.query = query;
            return this;
        }

        @Override
        CustomJdbcIO.Read.Builder<T> setRowMapper(final CustomJdbcIO.RowMapper<T> rowMapper) {
            this.rowMapper = rowMapper;
            return this;
        }

        @Override
        CustomJdbcIO.Read<T> build() {
            return new AutoValue_CustomJdbcIO_Read<T>(this.dataSourceConfiguration, this.query, this.rowMapper);
        }
    }
}
