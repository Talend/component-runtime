/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.runtime.documentation.component.source;

import static org.talend.runtime.documentation.component.configuration.TableDataSet.READ_ALL_RECORD_FROM_SERVER;
import static org.talend.runtime.documentation.component.service.http.TableApiClient.API_BASE;
import static org.talend.runtime.documentation.component.service.http.TableApiClient.API_VERSION;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.talend.runtime.documentation.component.configuration.TableDataSet;
import org.talend.runtime.documentation.component.messages.Messages;
import org.talend.runtime.documentation.component.service.http.TableApiClient;
import org.talend.sdk.component.api.base.BufferizedProducerSupport;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Producer;

public class MockTableSource implements Serializable {

    private final TableDataSet ds;

    private final Messages i18n;

    private BufferizedProducerSupport<JsonValue> bufferedReader;

    private TableApiClient tableAPI;

    public MockTableSource(@Option("ds") final TableDataSet ds, final Messages i18n, final TableApiClient tableAPI) {
        this.ds = ds;
        this.i18n = i18n;
        this.tableAPI = tableAPI;
    }

    @PostConstruct
    public void init() {
        tableAPI.base(ds.getDataStore().getUrlWithSlashEnding() + API_BASE + "/" + API_VERSION);
        bufferedReader = new BufferizedProducerSupport<>(() -> {
            if (ds.getMaxRecords() != READ_ALL_RECORD_FROM_SERVER && ds.getOffset() >= ds.getMaxRecords()) {
                return null;// stop reading from this source.
            }

            // Read next page from data set
            final JsonArray result = tableAPI
                    .getRecords(ds.getCommonConfig().getTableName().name(), ds.getDataStore().getAuthorizationHeader(),
                            ds.buildQuery(), ds.getCommonConfig().getFieldsCommaSeparated(), ds.getOffset(),
                            evalLimit(ds));

            // advance the data set offset
            if (ds.getOffset() < ds.getMaxRecords()) {
                ds.setOffset(ds.getOffset() + ds.getPageSize());
            }

            return result.iterator();
        });
    }

    private int evalLimit(final TableDataSet ds) {
        return ds.getOffset() + ds.getPageSize() <= ds.getMaxRecords() ? ds.getPageSize() : ds.getMaxRecords();
    }

    @Producer
    public JsonObject next() {
        final JsonValue next = bufferedReader.next();
        return next == null ? null : next.asJsonObject();
    }
}