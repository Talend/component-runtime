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
package org.talend.sdk.component.tcketl.domain.connector;

import java.util.List;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Palette entry built from the component index. The flows are not part of the index, so the role is only a
 * provisional hint until the {@link ConnectorDefinition} is loaded.
 */
public record ConnectorSummary(
        ConnectorRef ref,
        String familyId,
        String familyDisplayName,
        String displayName,
        List<String> categories,
        ConnectorRole provisionalRole,
        boolean streaming,
        String documentation
) {

    public ConnectorSummary {
        Preconditions.requireNonNull(ref, "ref");
        familyId = Preconditions.nullToEmpty(familyId);
        familyDisplayName = Preconditions.orDefault(familyDisplayName, ref.family());
        displayName = Preconditions.orDefault(displayName, ref.name());
        categories = Preconditions.immutableCopy(categories, "categories");
        Preconditions.requireNonNull(provisionalRole, "provisionalRole");
        documentation = Preconditions.nullToEmpty(documentation);
    }

    public ConnectorId id() {
        return ref.id();
    }

    public String family() {
        return ref.family();
    }

    public String name() {
        return ref.name();
    }
}
