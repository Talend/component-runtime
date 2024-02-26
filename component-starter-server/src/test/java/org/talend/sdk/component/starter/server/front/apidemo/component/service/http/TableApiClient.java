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
package org.talend.sdk.component.starter.server.front.apidemo.component.service.http;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpException;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.starter.server.front.apidemo.component.configuration.CommonConfig;
import org.talend.sdk.component.starter.server.front.apidemo.component.service.http.codec.InvalidContentDecoder;
import org.talend.sdk.component.starter.server.front.apidemo.component.service.http.codec.RecordSizeDecoder;

public interface TableApiClient extends HttpClient {

    String API_BASE = "api/now";

    String API_VERSION = "v2";

    String SYSPARM_SUPPRESS_PAGINATION_HEADER = "SYSPARM_SUPPRESS_PAGINATION_HEADER";

    String SYSPARM_OFFSET = "SYSPARM_OFFSET";

    String SYSPARM_LIMIT = "SYSPARM_LIMIT";

    String SYSPARM_EXCLUDE_REFERENCE_LINK = "SYSPARM_EXCLUDE_REFERENCE_LINK";

    String SYSPARM_QUERY = "SYSPARM_QUERY";

    String SYSPARM_FIELDS = "SYSPARM_FIELDS";

    String X_TOTAL_COUNT = "X-Total-Count";

    String X_NO_RESPONSE_BODY = "X-no-response-body";

    String AUTHORIZATION = "Authorization";

    String HEADER_CONTENT_TYPE = "Content-Type";

    @Request(path = "table/{tableName}")
    @Codec(decoder = { InvalidContentDecoder.class })
    @Documentation("read record from the table according to the data set definition")
    Response<JsonObject> get(@Path("tableName") String tableName, @Header(AUTHORIZATION) String auth,
            @Header(X_NO_RESPONSE_BODY) boolean noResponseBody, @Query(SYSPARM_QUERY) String query,
            @Query(SYSPARM_FIELDS) String fields, @Query(SYSPARM_OFFSET) int offset, @Query(SYSPARM_LIMIT) int limit,
            @Query(SYSPARM_EXCLUDE_REFERENCE_LINK) boolean excludeReferenceLink,
            @Query(SYSPARM_SUPPRESS_PAGINATION_HEADER) boolean suppressPaginationHeader);

    default JsonArray getRecords(String tableName, String auth, String query, String fields, int offset, int limit) {
        final Response<JsonObject> resp = get(tableName, auth, false, query, fields, offset, limit, true, true);
        if (resp.status() != 200) {
            throw new HttpException(resp);
        }

        final JsonObject response = resp.body();
        return response.getJsonArray("result");
    }

    default int count(String tableName, String auth, String query) {
        final Response<JsonObject> resp = get(tableName, auth, true, query, null, 0, 1, true, true);
        if (resp.status() != 200) {
            throw new HttpException(resp);
        }
        final List<String> totalCount = resp.headers().get(X_TOTAL_COUNT);
        if (totalCount == null) {
            return 0;
        }

        return Integer.parseInt(totalCount.iterator().next());
    }

    default void healthCheck(String auth) {
        getRecords(CommonConfig.Tables.incident.name(), auth, null, null, 0, 1);
    }

    @Request(path = "table/{tableName}")
    @Codec(decoder = { RecordSizeDecoder.class, InvalidContentDecoder.class })
    @Documentation("read record from the table according to the data set definition")
    long estimateRecordSize(@Path("tableName") String tableName, @Header(AUTHORIZATION) String auth,
            @Header(X_NO_RESPONSE_BODY) boolean noResponseBody, @Query(SYSPARM_QUERY) String query,
            @Query(SYSPARM_FIELDS) String fields, @Query(SYSPARM_OFFSET) int offset, @Query(SYSPARM_LIMIT) int limit,
            @Query(SYSPARM_EXCLUDE_REFERENCE_LINK) boolean excludeReferenceLink,
            @Query(SYSPARM_SUPPRESS_PAGINATION_HEADER) boolean suppressPaginationHeader);

    default long estimateRecordSize(String tableName, String auth, String query, String fields) {
        return estimateRecordSize(tableName, auth, false, query, fields, 0, 1, true, true);
    }

    @Request(path = "table/{tableName}", method = "POST")
    @Codec(decoder = { InvalidContentDecoder.class })
    @Documentation("Create a record to table")
    Response<JsonObject> create(@Path("tableName") String tableName, @Header(AUTHORIZATION) String auth,
            @Header(X_NO_RESPONSE_BODY) boolean noResponseBody, @Header(HEADER_CONTENT_TYPE) String contentType,
            @Query(SYSPARM_EXCLUDE_REFERENCE_LINK) boolean excludeReferenceLink, JsonObject record);

    default JsonObject create(String tableName, String auth, boolean noResponseBody, JsonObject record) {
        final Response<JsonObject> resp = create(tableName, auth, noResponseBody, "application/json", true, record);

        if (noResponseBody) {
            return null; // no content. can be returned when noResponseBody is a true
        }

        if (resp.status() == 201) {
            return resp.body().getJsonObject("result");
        }

        throw new HttpException(resp);
    }

    @Request(path = "table/{tableName}/{sysId}", method = "PUT")
    @Codec(decoder = { InvalidContentDecoder.class })
    @Documentation("update a record in table using it sys_id")
    Response<JsonObject> update(@Path("tableName") String tableName, @Path("sysId") String sysId,
            @Header(AUTHORIZATION) String auth, @Header(X_NO_RESPONSE_BODY) boolean noResponseBody,
            @Header(HEADER_CONTENT_TYPE) String contentType,
            @Query(SYSPARM_EXCLUDE_REFERENCE_LINK) boolean excludeReferenceLink, JsonObject record);

    default JsonObject update(String tableName, String sysId, String auth, boolean noResponseBody, JsonObject record) {
        final Response<JsonObject> resp =
                update(tableName, sysId, auth, noResponseBody, "application/json", true, record);

        if (resp.status() == 204) {
            return null; // no content. can be returned when noResponseBody is a true
        }

        if (resp.status() == 200) {
            return resp.body().getJsonObject("result");
        }

        throw new HttpException(resp);

    }

    @Request(path = "table/{tableName}/{sysId}", method = "DELETE")
    @Codec(decoder = { InvalidContentDecoder.class })
    @Documentation("delete a record from a table by it sys_id")
    Response<Void> delete(@Path("tableName") String tableName, @Path("sysId") String sysId,
            @Header(AUTHORIZATION) String auth);

    default void deleteRecordById(String tableName, String sysId, String auth) {
        final Response<?> resp = delete(tableName, sysId, auth);
        if (resp.status() != 204) {
            throw new HttpException(resp);
        }
    }
}
