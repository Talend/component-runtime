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
package org.talend.sdk.component.server.test.jdbc;

import java.io.Serializable;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.widget.ModuleList;
import org.talend.sdk.component.api.configuration.ui.widget.ReadOnly;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdbcConfig implements Serializable {

    @Option
    @Documentation(value = "Documentation for driver (jdbcconfig) doc.", tooltip = true)
    private String driver;

    @Option
    @Documentation(value = "Documentation for description (jdbcconfig) doc.", tooltip = true)
    private String description;

    @Option
    @Documentation(value = "Documentation for dependencies (jdbcconfig) doc.", tooltip = true)
    @ModuleList
    @ReadOnly
    private List<String> dependencies;
}
