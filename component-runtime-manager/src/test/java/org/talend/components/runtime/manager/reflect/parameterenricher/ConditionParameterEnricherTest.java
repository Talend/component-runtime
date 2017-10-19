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
package org.talend.components.runtime.manager.reflect.parameterenricher;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.junit.Test;
import org.talend.component.api.configuration.condition.ActiveIf;

public class ConditionParameterEnricherTest {

    @Test
    public void condition() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::if::target", "foo.bar");
                put("tcomp::condition::if::value", "true");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIf() {

            @Override
            public String target() {
                return "foo.bar";
            }

            @Override
            public String[] value() {
                return new String[] { "true" };
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIf.class;
            }
        }));
    }

    @Test
    public void conditionListValues() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::if::target", "foo.bar");
                put("tcomp::condition::if::value", "true,false");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIf() {

            @Override
            public String target() {
                return "foo.bar";
            }

            @Override
            public String[] value() {
                return new String[] { "true", "false" };
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIf.class;
            }
        }));
    }
}
