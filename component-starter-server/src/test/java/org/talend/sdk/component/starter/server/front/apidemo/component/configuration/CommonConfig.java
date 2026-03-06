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
package org.talend.sdk.component.starter.server.front.apidemo.component.configuration;

import static java.util.stream.Collectors.joining;
import static org.talend.sdk.component.api.configuration.ui.widget.Structure.Type.OUT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@GridLayout({ @GridLayout.Row({ "tableName" }) })
@GridLayout(names = GridLayout.FormType.ADVANCED, value = { @GridLayout.Row({ "fields" }) })
@Documentation("Table Configuration")
public class CommonConfig implements Serializable {

    public static final String PROPOSABLE_GET_TABLE_FIELDS = "GetTableFields";

    @Option
    @Documentation("The name of the table to be read")
    private Tables tableName = Tables.incident;

    @Option
    @Structure(discoverSchema = "guessTableSchema", type = OUT)
    @Proposable(PROPOSABLE_GET_TABLE_FIELDS)
    @Documentation("List of field names to return in the response.")
    private List<String> fields = new ArrayList<>();

    public String getFieldsCommaSeparated() {
        if (getFields() == null || getFields().isEmpty()) {
            return null;
        }

        return getFields().stream().collect(joining(","));
    }

    public enum Tables {
        incident,
        problem,
        change_request,
        sc_request,
        sc_cat_item,
    }

}
