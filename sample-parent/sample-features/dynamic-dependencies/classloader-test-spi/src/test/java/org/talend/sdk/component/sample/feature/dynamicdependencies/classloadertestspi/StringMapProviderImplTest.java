/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestspi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.StringMapTransformer;

class StringMapProviderImplTest {

    @Test
    void testSPI() {
        StringMapTransformer<String> stringMapTransformer = new StringMapTransformer<>(true);
        List<String> transform = stringMapTransformer.transform((s1, s2) -> s1 + ":" + s2);
        List<String> sorted = new ArrayList<>(transform);
        sorted.sort(String::compareTo);
        String collect = String.join("/", sorted);
        Assertions.assertEquals("key1:value1", collect);
    }

}