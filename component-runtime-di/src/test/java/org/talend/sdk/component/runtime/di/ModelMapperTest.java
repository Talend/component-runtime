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
package org.talend.sdk.component.runtime.di;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.talend.sdk.component.runtime.output.data.AccessorCache;
import org.talend.sdk.component.runtime.output.data.ObjectMapImpl;

import lombok.Data;

public class ModelMapperTest {

    @Test
    public void mapPerfectMatch() {
        final In in = new In();
        in.age = 23;
        in.name = "input";

        final OutMatching out =
                new ModelMapper().map(new ObjectMapImpl(null, in, new AccessorCache(null)), new OutMatching());
        assertEquals(23, out.age);
        assertEquals("input", out.name);
    }

    @Test
    public void mapWrapperMatch() {
        final In in = new In();
        in.age = 23;
        in.name = "input";

        final OutWrappers out =
                new ModelMapper().map(new ObjectMapImpl(null, in, new AccessorCache(null)), new OutWrappers());
        assertEquals(23, out.age.intValue());
        assertEquals("input", out.name);
    }

    @Test
    public void mapWrapperToPrimitive() {
        final InWrapper in = new InWrapper();
        in.age = null;
        in.name = "input";

        final OutMatching out =
                new ModelMapper().map(new ObjectMapImpl(null, in, new AccessorCache(null)), new OutMatching());
        assertEquals(0, out.age);
        assertEquals("input", out.name);
    }

    @Data
    public static class In {

        private String name;

        private int age;
    }

    @Data
    public static class InWrapper {

        private String name;

        private Integer age;
    }

    public static class OutMatching {

        public String name;

        public int age;
    }

    public static class OutWrappers {

        public String name;

        public Integer age;
    }
}
