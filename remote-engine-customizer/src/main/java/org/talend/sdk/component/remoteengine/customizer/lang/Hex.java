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
package org.talend.sdk.component.remoteengine.customizer.lang;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Hex {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String hex(final byte[] data) {
        final StringBuilder out = new StringBuilder(data.length * 2);
        for (final byte b : data) {
            out.append(HEX_CHARS[b >> 4 & 15]).append(HEX_CHARS[b & 15]);
        }
        return out.toString();
    }
}
