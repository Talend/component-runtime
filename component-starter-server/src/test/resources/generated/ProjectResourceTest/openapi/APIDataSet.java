package com.test.openapi.dataset;

import java.io.Serializable;

import javax.json.JsonObject;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.configuration.ui.widget.Code;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.api.meta.Documentation;

import com.test.openapi.connection.APIConnection;
import com.test.openapi.client.APIClient;

@Version
@Icon(Icon.IconType.STAR) // todo use CUSTOM
@DataSet("APIDataSet")
@Documentation("Enables to select the API to call.")
@OptionsOrder({
    "connection",
    "api",
    "configurationVersion",
    "segment",
    "language",
    "id",
    "body"
})
public class APIDataSet implements Serializable {
    @Option
    @Documentation("The API connection.")
    private APIConnection connection;

    @Option
    @Documentation("The API to use for this call.")
    private APIType api;

    @Option
    @ActiveIf(target = "api", value = { "migrateComponent" })
    @Documentation("configurationVersion value.")
    private int configurationVersion;

    @Option
    @ActiveIf(target = "api", value = { "getDocumentation" })
    @Documentation("segment value.")
    private String segment;

    @Option
    @ActiveIf(target = "api", value = { "getDocumentation" })
    @Documentation("language value.")
    private String language;

    @Option
    @DefaultValue("id_1")
    @ActiveIf(target = "api", value = { "migrateComponent", "getDocumentation" })
    @Documentation("id value.")
    private String id;

    @Option
    @DefaultValue("\"{}\"")
    @Code("javascript")
    @ActiveIf(target = "api", value = { "migrateComponent" })
    @Documentation("body value.")
    private String body;

    public int getConfigurationVersion() {
        return configurationVersion;
    }

    public String getSegment() {
        return segment;
    }

    public String getLanguage() {
        return language;
    }

    public String getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public APIConnection getConnection() {
        return connection;
    }

    public APIType getAPI() {
        return api;
    }

    public enum APIType {
        migrateComponent {
            @Override
            public Response<JsonObject> call(final APIDataSet config, final APIClient client) {
                validateUrl(config);
                client.base(config.getConnection().getBaseUrl());
                return client.migrateComponent(config.getBody(), config.getId(), config.getConfigurationVersion());
            }
        },
        getDocumentation {
            @Override
            public Response<JsonObject> call(final APIDataSet config, final APIClient client) {
                validateUrl(config);
                client.base(config.getConnection().getBaseUrl());
                return client.getDocumentation(config.getId(), config.getLanguage(), config.getSegment());
            }
        },
        getEnvironment {
            @Override
            public Response<JsonObject> call(final APIDataSet config, final APIClient client) {
                validateUrl(config);
                client.base(config.getConnection().getBaseUrl());
                return client.getEnvironment();
            }
        };

        protected void validateUrl(final APIDataSet config) {
            if (config.getConnection() == null || config.getConnection().getBaseUrl() == null) {
                throw new IllegalArgumentException("No base url set");
            }
        }

        public abstract Response<JsonObject> call(APIDataSet config, APIClient client);
    }
}