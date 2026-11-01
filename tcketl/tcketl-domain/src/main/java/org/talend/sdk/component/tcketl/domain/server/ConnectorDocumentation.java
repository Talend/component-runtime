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
package org.talend.sdk.component.tcketl.domain.server;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/** Documentation of a connector: its markup {@code format}, e.g. {@code asciidoc}, and the source {@code text}. */
public record ConnectorDocumentation(String format, String text) {

    public ConnectorDocumentation {
        format = Preconditions.nullToEmpty(format);
        text = Preconditions.nullToEmpty(text);
    }
}
