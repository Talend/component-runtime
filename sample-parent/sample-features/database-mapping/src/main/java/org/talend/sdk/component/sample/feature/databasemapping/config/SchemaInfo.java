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
package org.talend.sdk.component.sample.feature.databasemapping.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@GridLayout({ @GridLayout.Row({ "label", "originalDbColumnName", "key", "type", "talendType", "nullable", "pattern",
        "length", "precision", "defaultValue", "comment" }) })
@Documentation("Schema definition.")
public class SchemaInfo implements Serializable {

    @Option
    @Documentation("Column name.")
    private String label;

    @Option
    @Documentation("Original column name like db column name.")
    private String originalDbColumnName;

    @Option
    @Documentation("Is it a Key column.")
    private boolean key;

    @Option
    @Documentation("DB Type like VARCHAR, or type in source system.")
    private String type;

    @Option
    @Documentation("Talend type such as id_String.")
    private String talendType;

    @Option
    @Documentation("Is it a Nullable column.")
    private boolean nullable;

    @Option
    @Documentation("Pattern used for datetime processing.")
    private String pattern = "yyyy-MM-dd HH:mm";

    @Option
    @Documentation("Length or decimal type precision.")
    private Integer length;

    @Option
    @Documentation("Precision or decimal type scale.")
    private Integer precision;

    @Option
    @Documentation("Default value.")
    private String defaultValue;

    @Option
    @Documentation("Comment.")
    private String comment;
}
