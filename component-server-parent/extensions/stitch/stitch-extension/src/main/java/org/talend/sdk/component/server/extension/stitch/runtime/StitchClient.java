/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch.runtime;

import javax.json.JsonObject;

import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Request;

public interface StitchClient extends HttpClient {

    @Request(method = "POST", path = "/sources")
    JsonObject createSource(@Header("Authorization") String bearer, @Header("Content-Type") String contentType,
            JsonObject payload);

    @Request(path = "/sources/{source_id}/streams/{stream_id}")
    JsonObject getSchema(@Header("Authorization") String bearer, @Header("Content-Type") String contentType,
            @Path("source_id") String sourceId, @Path("stream_id") String streamId);

    @Request(method = "PUT", path = "/sources/{source_id}/streams/metadata")
    JsonObject customizeSchema(@Header("Authorization") String bearer, @Header("Content-Type") String contentType,
            @Path("source_id") String sourceId);

    @Request(method = "DELETE", path = "/sources/{source_id}")
    JsonObject deleteSource(@Header("Authorization") String bearer, @Header("Content-Type") String contentType,
            @Path("source_id") String sourceId, JsonObject payload);
}
