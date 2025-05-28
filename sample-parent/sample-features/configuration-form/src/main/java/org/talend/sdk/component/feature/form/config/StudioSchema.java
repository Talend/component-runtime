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
package org.talend.sdk.component.feature.form.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "label" }),
        @GridLayout.Row({ "originalDbColumnName" }),
        @GridLayout.Row({ "key" }),
        @GridLayout.Row({ "type" }),
        @GridLayout.Row({ "talendType" }),
        @GridLayout.Row({ "nullable" }),
        @GridLayout.Row({ "pattern" }),
        @GridLayout.Row({ "length" }),
        @GridLayout.Row({ "precision" }),
        @GridLayout.Row({ "defaultValue" }),
        @GridLayout.Row({ "comment" })
})
public class StudioSchema implements Serializable {

    @Option
    @Documentation("The schema label.")
    private String label;

    @Option
    @Documentation("The schema attribute original column name.")
    private String originalDbColumnName;

    @Option
    @Documentation("The schema attribute key flag.")
    private boolean key;

    @Option
    @Documentation("The schema attribute db type.")
    private String type;

    @Option
    @Documentation("The schema attribute java type.")
    private String talendType;

    @Option
    @Documentation("The schema attribute nullable flag.")
    private boolean nullable;

    @Option
    @Documentation("The schema attribute data pattern.")
    private String pattern;

    @Option
    @Documentation("The schema attribute length.")
    private int length;

    @Option
    @Documentation("The schema attribute precision.")
    private int precision;

    @Option
    @Documentation("The schema attribute default value.")
    private String defaultValue;

    @Option
    @Documentation("The schema attribute comment.")
    private String comment;

}
