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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.talend.component.api.configuration.type.DataSet;

public class ConfigurationTypeParameterEnricherTest {

    @Test
    public void readConfigTypes() {
        final ConfigurationTypeParameterEnricher enricher = new ConfigurationTypeParameterEnricher();
        assertEquals(singletonMap("dataset", "test"), enricher.onParameterAnnotation("testParam", null, new DataSet() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return DataSet.class;
            }

            @Override
            public String value() {
                return "test";
            }
        }));
        assertEquals(emptyMap(), enricher.onParameterAnnotation("testParam", null, new Override() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Override.class;
            }
        }));
    }
}
