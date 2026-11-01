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

/**
 * What the server tells about itself. {@code stamp} changes when the set of deployed components may have changed
 * (the server has no ETag), it is opaque and must only be compared for equality.
 */
public record ServerStatus(String version, String commit, String stamp) {

    public ServerStatus {
        version = Preconditions.nullToEmpty(version);
        commit = Preconditions.nullToEmpty(commit);
        stamp = Preconditions.nullToEmpty(stamp);
    }
}
