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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.Pattern;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.constraint.Uniques;

class ValidationParameterEnricherTest {

    private final ValidationParameterEnricher enricher = new ValidationParameterEnricher();

    @Test
    void minValue() {
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
    void maxValue() {
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
    void minCollection() {
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
    void maxCollection() {
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
    void minlength() {
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
    void maxLength() {
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
    void pattern() {
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
    void required() {
        assertEquals(singletonMap("tcomp::validation::required", "true"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Required() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Required.class;
                    }
                }));
    }

    @Test
    void unique() {
        assertEquals(singletonMap("tcomp::validation::uniqueItems", "true"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Uniques() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Uniques.class;
                    }
                }));
    }

    @Test
    void enumType() {
        assertEquals(singletonMap("tcomp::validation::uniqueItems", "true"),
                enricher.onParameterAnnotation("testParam", Collection.class, new Uniques() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Uniques.class;
                    }
                }));
    }

}
