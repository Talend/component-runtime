package com.test.openapi.client;

import javax.json.JsonObject;

import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;

public interface APIClient extends HttpClient {

    @Request(method = "POST", path = "/api/v1/component/migrate/{id}/{configurationVersion}")
    Response<JsonObject> migrateComponent(@Path("id") String id, @Path("configurationVersion") int configurationVersion);

    @Request(method = "GET", path = "/api/v1/documentation/component/{id}")
    Response<JsonObject> getDocumentation(@Path("id") String id, @Query("language") String language, @Query("segment") String segment);

    @Request(method = "GET", path = "/api/v1/environment")
    Response<JsonObject> getEnvironment();
}