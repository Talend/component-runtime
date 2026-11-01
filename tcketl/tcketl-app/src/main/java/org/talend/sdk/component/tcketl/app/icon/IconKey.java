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
package org.talend.sdk.component.tcketl.app.icon;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;

/** Identifies an icon of the server: the one of a connector, or the one of its family. */
public record IconKey(Kind kind, String id) {

    public static IconKey component(final ConnectorId id) {
        return new IconKey(Kind.COMPONENT, id.value());
    }

    public static IconKey family(final String familyId) {
        return new IconKey(Kind.FAMILY, familyId);
    }

    public enum Kind {
        COMPONENT,
        FAMILY
    }
}
