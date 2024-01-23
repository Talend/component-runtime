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
package org.talend.sdk.component.server.front.model;

import java.util.Collection;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentDetail {

    private ComponentId id;

    private String displayName;

    private String icon;

    private String type;

    private int version;

    private Collection<SimplePropertyDefinition> properties;

    private Collection<ActionReference> actions;

    /**
     * Input flow (connection) names
     */
    private Collection<String> inputFlows;

    /**
     * Output flow (connection) names
     */
    private Collection<String> outputFlows;

    private Collection<Link> links;

    private Map<String, String> metadata;
}
