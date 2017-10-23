/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.junit.Test;
import org.talend.component.api.configuration.ui.OptionsOrder;
import org.talend.component.api.configuration.ui.layout.GridLayout;
import org.talend.component.api.configuration.ui.layout.GridLayouts;
import org.talend.component.api.configuration.ui.layout.HorizontalLayout;
import org.talend.component.api.configuration.ui.widget.Code;
import org.talend.component.api.configuration.ui.widget.Credential;

public class UiParameterEnricherTest {

    private final UiParameterEnricher enricher = new UiParameterEnricher();

    @Test
    public void gridLayouts() {
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
    public void gridLayout() {
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
    public void credential() {
        assertEquals(singletonMap("tcomp::ui::credential", "true"),
                enricher.onParameterAnnotation("testParam", String.class, new Credential() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Credential.class;
                    }
                }));
    }

    @Test
    public void code() {
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
    public void layout() {
        assertEquals(singletonMap("tcomp::ui::horizontallayout", "true"),
                enricher.onParameterAnnotation("testParam", Object.class, new HorizontalLayout() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return HorizontalLayout.class;
                    }
                }));
    }

    @Test
    public void order() {
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
}
