/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.connector;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;

import org.talend.core.model.process.INode;
import org.talend.core.runtime.IAdditionalInfo;
import org.talend.designer.core.model.components.NodeConnector;

class TacokitConnector extends NodeConnector implements IAdditionalInfo {

    private final Map<String, Object> infos = new HashMap<>();

    TacokitConnector(final INode parentNode) {
        super(parentNode);
    }

    void setInternalName(final String name) {
        putInfo("internalName", name);
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public Object getInfo(final String s) {
        return infos.get(s);
    }

    @Override
    public void putInfo(final String s, final Object o) {
        infos.put(s, o);
    }

    @Override
    public void onEvent(final String s, final Object... objects) {
        // no-op
    }

    @Override
    public void cloneAddionalInfoTo(final IAdditionalInfo iAdditionalInfo) {
        ofNullable(iAdditionalInfo).ifPresent(collector -> infos.forEach(collector::putInfo));
    }
}
