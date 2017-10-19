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

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.junit.Test;
import org.talend.component.api.configuration.constraint.Max;
import org.talend.component.api.configuration.constraint.Min;
import org.talend.component.api.configuration.constraint.Pattern;
import org.talend.component.api.configuration.constraint.Required;
import org.talend.component.api.configuration.constraint.Uniques;

public class ValidationParameterEnricherTest {

    private final ValidationParameterEnricher enricher = new ValidationParameterEnricher();

    @Test
    public void minValue() {
        assertEquals(singletonMap("tcomp::validation::min", "5.0"),
                enricher.onParameterAnnotation("testParam", int.class, new Min() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Min.class;
                    }

                    @Override
                    public double value() {
                        return 5;
                    }
                }));
    }

    @Test
    public void maxValue() {
        assertEquals(singletonMap("tcomp::validation::max", "5.0"),
                enricher.onParameterAnnotation("testParam", int.class, new Max() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Max.class;
                    }

                    @Override
                    public double value() {
                        return 5;
                    }
                }));
    }

    @Test
    public void minCollection() {
        assertEquals(singletonMap("tcomp::validation::minItems", "5.0"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Min() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Min.class;
                    }

                    @Override
                    public double value() {
                        return 5;
                    }
                }));
    }

    @Test
    public void maxCollection() {
        assertEquals(singletonMap("tcomp::validation::maxItems", "5.0"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Max() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Max.class;
                    }

                    @Override
                    public double value() {
                        return 5;
                    }
                }));
    }

    @Test
    public void minlength() {
        assertEquals(singletonMap("tcomp::validation::minLength", "5.0"),
                enricher.onParameterAnnotation("testParam", String.class, new Min() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Min.class;
                    }

                    @Override
                    public double value() {
                        return 5;
                    }
                }));
    }

    @Test
    public void maxLength() {
        assertEquals(singletonMap("tcomp::validation::maxLength", "5.0"),
                enricher.onParameterAnnotation("testParam", String.class, new Max() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Max.class;
                    }

                    @Override
                    public double value() {
                        return 5;
                    }
                }));
    }

    @Test
    public void pattern() {
        assertEquals(singletonMap("tcomp::validation::pattern", "test"),
                enricher.onParameterAnnotation("testParam", String.class, new Pattern() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Pattern.class;
                    }

                    @Override
                    public String value() {
                        return "test";
                    }
                }));
    }

    @Test
    public void required() {
        assertEquals(singletonMap("tcomp::validation::required", "true"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Required() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Required.class;
                    }
                }));
    }

    @Test
    public void unique() {
        assertEquals(singletonMap("tcomp::validation::uniqueItems", "true"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Uniques() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Uniques.class;
                    }
                }));
    }

    @Test
    public void enumType() {
        assertEquals(singletonMap("tcomp::validation::uniqueItems", "true"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Uniques() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Uniques.class;
                    }
                }));
    }

}
