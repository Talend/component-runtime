package org.talend.sdk.component.runtime.manager.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class IdGenerator {

    /**
     * /!\  keep this algorithm private for now and don't assume it is reversible, we can revise it to something more compressed later
     *
     * @param args
     * @return a {@link Base64} url encoded string from the strings parameter joined by #
     */
    public static String get(String... args) {

        if (args == null || args.length == 0) {
            return null;
        }

        return Base64.getUrlEncoder().withoutPadding()
                     .encodeToString(Arrays.stream(args).collect(Collectors.joining("#")).getBytes(StandardCharsets.UTF_8));
    }

}
