/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.base;

import java.io.Serializable;

public abstract class Named implements Lifecycle, Serializable {

    private String name;

    private String rootName;

    private String plugin;

    protected Named(final String rootName, final String name, final String plugin) {
        this.rootName = rootName;
        this.name = name;
        this.plugin = plugin;
    }

    protected Named() {
        // no-op
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return rootName;
    }

    @Override
    public String name() {
        return name;
    }
}
