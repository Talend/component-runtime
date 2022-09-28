/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import javax.sql.DataSource;

@Generated("from_com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_CustomJdbcIO_DataSourceConfiguration extends CustomJdbcIO.DataSourceConfiguration {

    private final String driverClassName;

    private final String url;

    private final String username;

    private final String password;

    private final String connectionProperties;

    private final DataSource dataSource;

    private AutoValue_CustomJdbcIO_DataSourceConfiguration(final String driverClassName, final String url,
            final String username, final String password, final String connectionProperties,
            final DataSource dataSource) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.connectionProperties = connectionProperties;
        this.dataSource = dataSource;
    }

    @Override
    String getDriverClassName() {
        return driverClassName;
    }

    @Override
    String getUrl() {
        return url;
    }

    @Override
    String getUsername() {
        return username;
    }

    @Override
    String getPassword() {
        return password;
    }

    @Override
    String getConnectionProperties() {
        return connectionProperties;
    }

    @Override
    DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String toString() {
        return "DataSourceConfiguration{" + "driverClassName=" + driverClassName + ", " + "url=" + url + ", "
                + "username=" + username + ", " + "password=" + password + ", " + "connectionProperties="
                + connectionProperties + ", " + "dataSource=" + dataSource + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CustomJdbcIO.DataSourceConfiguration) {
            CustomJdbcIO.DataSourceConfiguration that = (CustomJdbcIO.DataSourceConfiguration) o;
            return ((this.driverClassName == null) ? (that.getDriverClassName() == null)
                    : this.driverClassName.equals(that.getDriverClassName()))
                    && ((this.url == null) ? (that.getUrl() == null) : this.url.equals(that.getUrl()))
                    && ((this.username == null) ? (that.getUsername() == null)
                            : this.username.equals(that.getUsername()))
                    && ((this.password == null) ? (that.getPassword() == null)
                            : this.password.equals(that.getPassword()))
                    && ((this.connectionProperties == null) ? (that.getConnectionProperties() == null)
                            : this.connectionProperties.equals(that.getConnectionProperties()))
                    && ((this.dataSource == null) ? (that.getDataSource() == null)
                            : this.dataSource.equals(that.getDataSource()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (driverClassName == null) ? 0 : this.driverClassName.hashCode();
        h *= 1000003;
        h ^= (url == null) ? 0 : this.url.hashCode();
        h *= 1000003;
        h ^= (username == null) ? 0 : this.username.hashCode();
        h *= 1000003;
        h ^= (password == null) ? 0 : this.password.hashCode();
        h *= 1000003;
        h ^= (connectionProperties == null) ? 0 : this.connectionProperties.hashCode();
        h *= 1000003;
        h ^= (dataSource == null) ? 0 : this.dataSource.hashCode();
        return h;
    }

    @Override
    CustomJdbcIO.DataSourceConfiguration.Builder builder() {
        return new Builder(this);
    }

    static final class Builder extends CustomJdbcIO.DataSourceConfiguration.Builder {

        private String driverClassName;

        private String url;

        private String username;

        private String password;

        private String connectionProperties;

        private DataSource dataSource;

        Builder() {
            // no-op
        }

        private Builder(final CustomJdbcIO.DataSourceConfiguration source) {
            this.driverClassName = source.getDriverClassName();
            this.url = source.getUrl();
            this.username = source.getUsername();
            this.password = source.getPassword();
            this.connectionProperties = source.getConnectionProperties();
            this.dataSource = source.getDataSource();
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration.Builder setDriverClassName(final String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration.Builder setUrl(final String url) {
            this.url = url;
            return this;
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration.Builder setUsername(final String username) {
            this.username = username;
            return this;
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration.Builder setPassword(final String password) {
            this.password = password;
            return this;
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration.Builder setConnectionProperties(final String connectionProperties) {
            this.connectionProperties = connectionProperties;
            return this;
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration.Builder setDataSource(final DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        @Override
        CustomJdbcIO.DataSourceConfiguration build() {
            return new AutoValue_CustomJdbcIO_DataSourceConfiguration(this.driverClassName, this.url, this.username,
                    this.password, this.connectionProperties, this.dataSource);
        }
    }

}
