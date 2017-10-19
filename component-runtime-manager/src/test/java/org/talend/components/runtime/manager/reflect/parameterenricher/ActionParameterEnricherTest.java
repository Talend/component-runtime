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
import org.talend.component.api.configuration.action.Discoverable;
import org.talend.component.api.configuration.action.Proposable;

public class ActionParameterEnricherTest {

    @Test
    public void condition() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::action::dynamic_values", "test");
            }
        }, new ActionParameterEnricher().onParameterAnnotation("testParam", String.class, new Proposable() {

            @Override
            public String value() {
                return "test";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Proposable.class;
            }
        }));
    }

    @Test
    public void discoverable() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::action::schema::binding", "ALL");
                put("tcomp::action::schema", "test");
            }
        }, new ActionParameterEnricher().onParameterAnnotation("testParam", String.class, new Discoverable() {

            @Override
            public String value() {
                return "test";
            }

            @Override
            public Binding binding() {
                return Binding.ALL;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Discoverable.class;
            }
        }));
    }
}
