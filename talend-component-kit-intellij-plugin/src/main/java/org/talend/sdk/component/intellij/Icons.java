/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.intellij;

import static com.intellij.openapi.util.IconLoader.getIcon;

import javax.swing.Icon;

public final class Icons {

    public static final Icon TACOKIT = getIcon("/tacokit.png", Icons.class);

    public static final Icon TALEND = getIcon("/talend_logo_.png", Icons.class);

    private Icons() {
        // no-op
    }
}
