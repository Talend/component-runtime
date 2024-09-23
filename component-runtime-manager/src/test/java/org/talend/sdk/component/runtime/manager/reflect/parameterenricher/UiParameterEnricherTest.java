/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.Hidden;
import org.talend.sdk.component.api.configuration.ui.OptionsOrder;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayouts;
import org.talend.sdk.component.api.configuration.ui.layout.HorizontalLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Code;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;
import org.talend.sdk.component.api.configuration.ui.widget.DateTime;
import org.talend.sdk.component.api.configuration.ui.widget.ModuleList;
import org.talend.sdk.component.api.configuration.ui.widget.ReadOnly;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;

class UiParameterEnricherTest {

    private final UiParameterEnricher enricher = new UiParameterEnricher();

    @Test
    void gridLayouts() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::ui::gridlayout::MAIN::value", "first|second,third");
                put("tcomp::ui::gridlayout::ADVANCED::value", "another");
            }
        }, enricher.onParameterAnnotation("testParam", String.class, new GridLayouts() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return GridLayouts.class;
            }

            @Override
            public GridLayout[] value() {
                return new GridLayout[] { new GridLayout() {

                    @Override
                    public Row[] value() {
                        return new Row[] { new Row() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return Row.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "first" };
                            }
                        }, new Row() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return Row.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "second", "third" };
                            }
                        } };
                    }

                    @Override
                    public String[] names() {
                        return new String[] { "MAIN" };
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return GridLayout.class;
                    }
                }, new GridLayout() {

                    @Override
                    public Row[] value() {
                        return new Row[] { new Row() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return Row.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "another" };
                            }
                        } };
                    }

                    @Override
                    public String[] names() {
                        return new String[] { "ADVANCED" };
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return GridLayout.class;
                    }
                } };
            }
        }));
    }

    @Test
    void gridLayout() {
        assertEquals(singletonMap("tcomp::ui::gridlayout::MAIN::value", "first|second,third"),
                enricher.onParameterAnnotation("testParam", String.class, new GridLayout() {

                    @Override
                    public Row[] value() {
                        return new Row[] { new Row() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return Row.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "first" };
                            }
                        }, new Row() {

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return Row.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "second", "third" };
                            }
                        } };
                    }

                    @Override
                    public String[] names() {
                        return new String[] { "MAIN" };
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return GridLayout.class;
                    }
                }));
    }

    @Test
    void selector() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::ui::structure::value", "__default__");
                put("tcomp::ui::structure::type", "IN");
                put("tcomp::ui::structure::discoverSchema", "guess");
            }
        }, enricher.onParameterAnnotation("testParam", String.class, new Structure() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Structure.class;
            }

            @Override
            public String value() {
                return "__default__";
            }

            @Override
            public String discoverSchema() {
                return "guess";
            }

            @Override
            public Type type() {
                return Type.IN;
            }
        }));
    }

    @ParameterizedTest
    @ValueSource(classes = { ZonedDateTime.class, LocalDateTime.class, LocalDate.class })
    void datetime(final Class<?> type) {
        final HashMap<String, String> datetime = new HashMap<String, String>() {

            {
                put("tcomp::ui::datetime", type.getSimpleName().replace("Local", "").toLowerCase(ROOT));
                put("tcomp::ui::datetime::dateFormat", "YYYY-MM-DD");
                put("tcomp::ui::datetime::useSeconds", "false");
                put("tcomp::ui::datetime::useUTC", "false");
            }
        };
        final HashMap<String, String> date = new HashMap<String, String>() {

            {
                put("tcomp::ui::datetime", type.getSimpleName().replace("Local", "").toLowerCase(ROOT));
                put("tcomp::ui::datetime::dateFormat", "YYYY-MM-DD");
            }
        };
        assertEquals(type == LocalDate.class ? date : datetime,
                enricher.onParameterAnnotation("testParam", type, new DateTime() {

                    @Override
                    public String dateFormat() {
                        return "YYYY-MM-DD";
                    }

                    @Override
                    public boolean useSeconds() {
                        return false;
                    }

                    @Override
                    public boolean useUTC() {
                        return false;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return DateTime.class;
                    }
                }));

        assertThrows(IllegalArgumentException.class,
                () -> enricher.onParameterAnnotation("testParam", String.class, new DateTime() {

                    @Override
                    public String dateFormat() {
                        return "YYYY/MM/DD";
                    }

                    @Override
                    public boolean useSeconds() {
                        return true;
                    }

                    @Override
                    public boolean useUTC() {
                        return true;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return DateTime.class;
                    }
                }));
    }

    @Test
    void credential() {
        assertEquals(singletonMap("tcomp::ui::credential", "true"),
                enricher.onParameterAnnotation("testParam", String.class, new Credential() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Credential.class;
                    }
                }));
    }

    @Test
    void moduleList() {
        assertEquals(singletonMap("tcomp::ui::modulelist", "true"),
                enricher.onParameterAnnotation("testParam", String.class, new ModuleList() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return ModuleList.class;
                    }

                }));
    }

    @Test
    void code() {
        assertEquals(singletonMap("tcomp::ui::code::value", "groovy"),
                enricher.onParameterAnnotation("testParam", Object.class, new Code() {

                    @Override
                    public String value() {
                        return "groovy";
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Code.class;
                    }
                }));
    }

    @Test
    void layout() {
        assertEquals(singletonMap("tcomp::ui::horizontallayout", "true"),
                enricher.onParameterAnnotation("testParam", Object.class, new HorizontalLayout() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return HorizontalLayout.class;
                    }
                }));
    }

    @Test
    void order() {
        assertEquals(singletonMap("tcomp::ui::optionsorder::value", "username,password"),
                enricher.onParameterAnnotation("testParam", Object.class, new OptionsOrder() {

                    @Override
                    public String[] value() {
                        return new String[] { "username", "password" };
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return OptionsOrder.class;
                    }
                }));
    }

    @Test
    void defaultValue() {
        assertEquals(singletonMap("tcomp::ui::defaultvalue::value", "foo"),
                enricher.onParameterAnnotation("testParam", Object.class, new DefaultValue() {

                    @Override
                    public String value() {
                        return "foo";
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return DefaultValue.class;
                    }
                }));
    }

    @Test
    void hidden() {
        assertEquals(singletonMap("tcomp::ui::hidden", "true"),
                enricher.onParameterAnnotation("testParam", String.class, new Hidden() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Hidden.class;
                    }
                }));
    }

    @Test
    void defaultValueConfiguration() {
        assertTrue(enricher.withContext(new BaseParameterEnricher.Context(new LocalConfiguration() {

            @Override
            public String get(final String key) {
                return "key=" + key;
            }

            @Override
            public Set<String> keys() {
                return emptySet();
            }
        }), () -> {
            assertEquals(singletonMap("tcomp::ui::defaultvalue::value", "key=foo"),
                    enricher.onParameterAnnotation("testParam", Object.class, new DefaultValue() {

                        @Override
                        public String value() {
                            return "local_configuration:foo";
                        }

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return DefaultValue.class;
                        }
                    }));
            return true;
        }));
    }

    @Test
    void readOnly() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::ui::readonly", "true");
            }
        }, enricher.onParameterAnnotation("testParam", String.class, new ReadOnly() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return ReadOnly.class;
            }

        }));
    }

}
