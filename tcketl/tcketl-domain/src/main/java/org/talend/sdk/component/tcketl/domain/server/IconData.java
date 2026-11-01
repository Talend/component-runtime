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

import java.util.Arrays;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/** An icon as served by the component server: raw bytes and their media type, e.g. {@code image/svg+xml}. */
public record IconData(byte[] content, String mediaType) {

    public IconData {
        content = Preconditions.requireNonNull(content, "content").clone();
        mediaType = Preconditions.nullToEmpty(mediaType);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public boolean isSvg() {
        return mediaType.startsWith("image/svg");
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof IconData icon && mediaType.equals(icon.mediaType)
                && Arrays.equals(content, icon.content);
    }

    @Override
    public int hashCode() {
        return 31 * mediaType.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "IconData[" + mediaType + ", " + content.length + " bytes]";
    }
}
