/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.configuration.dependency;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;

import lombok.Data;

@Data
public class ConnectorReference implements Serializable {

    /**
     * Family of referenced connector.
     */
    @Option
    private String family;

    /**
     * Name of referenced connector.
     */
    @Option
    private String name;

    /**
     * maven reference of referenced connector (as gav).
     * exemple : org.talend.components:rest:1.29.0
     */
    @Option
    private String mavenReferences;
}
