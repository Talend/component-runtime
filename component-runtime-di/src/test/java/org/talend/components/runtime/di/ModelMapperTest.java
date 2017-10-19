// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.di;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.talend.components.runtime.output.data.AccessorCache;
import org.talend.components.runtime.output.data.ObjectMapImpl;

import lombok.Data;

public class ModelMapperTest {

    @Test
    public void mapPerfectMatch() {
        final In in = new In();
        in.age = 23;
        in.name = "input";

        final OutMatching out = new ModelMapper().map(new ObjectMapImpl(null, in, new AccessorCache(null)), new OutMatching());
        assertEquals(23, out.age);
        assertEquals("input", out.name);
    }

    @Test
    public void mapWrapperMatch() {
        final In in = new In();
        in.age = 23;
        in.name = "input";

        final OutWrappers out = new ModelMapper().map(new ObjectMapImpl(null, in, new AccessorCache(null)), new OutWrappers());
        assertEquals(23, out.age.intValue());
        assertEquals("input", out.name);
    }

    @Test
    public void mapWrapperToPrimitive() {
        final InWrapper in = new InWrapper();
        in.age = null;
        in.name = "input";

        final OutMatching out = new ModelMapper().map(new ObjectMapImpl(null, in, new AccessorCache(null)), new OutMatching());
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
