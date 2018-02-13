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

import org.talend.core.model.process.INode;
import org.talend.designer.core.model.components.NodeConnector;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class TaCoKitNodeConnector extends NodeConnector {

    private boolean isInput = false;

    public TaCoKitNodeConnector(final INode parentNode) {
        super(parentNode);
    }

    public void setInput(final boolean isInput) {
        this.isInput = isInput;
    }

    public boolean isInput() {
        return isInput;
    }

}
