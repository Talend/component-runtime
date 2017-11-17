/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.design.extension.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.runtime.manager.ParameterMeta;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Config {

    private Config parent;

    private String name;

    private String displayName;

    private String path;

    private String icon;

    private ConfigKey key;

    private List<ParameterMeta> properties = new ArrayList<>();

    private List<Config> childConfigs = new ArrayList<>();
}
