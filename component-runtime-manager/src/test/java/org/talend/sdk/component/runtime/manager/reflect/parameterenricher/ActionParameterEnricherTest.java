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

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.action.BuiltInSuggestable;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.action.Updatable;

class ActionParameterEnricherTest {

    @Test
    void update() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::action::update", "test");
                put("tcomp::action::update::parameters", ".,foo,/bar/dummy");
                put("tcomp::action::update::after", "propertyX");
                put("tcomp::action::update::activeIf", "target = \"authenticationKind\", value = \"BASIC_AUTH\"");
            }
        }, new ActionParameterEnricher().onParameterAnnotation("testParam", String.class, new Updatable() {

            @Override
            public String value() {
                return "test";
            }

            @Override
            public String after() {
                return "propertyX";
            }

            @Override
            public String[] parameters() {
                return new String[] { ".", "foo", "/bar/dummy" };
            }

            @Override
            public String activeIf() {
                return "target = \"authenticationKind\", value = \"BASIC_AUTH\"";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Updatable.class;
            }
        }));
    }

    @Test
    void builtInSuggestion() {
        assertEquals(singletonMap("tcomp::action::built_in_suggestable", "INCOMING_SCHEMA_ENTRY_NAMES"),
                new ActionParameterEnricher()
                        .onParameterAnnotation("testParam", String.class, new BuiltInSuggestable() {

                            @Override
                            public Name value() {
                                return Name.INCOMING_SCHEMA_ENTRY_NAMES;
                            }

                            @Override
                            public String name() {
                                return "";
                            }

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return BuiltInSuggestable.class;
                            }
                        }));
    }

    @Test
    void builtInSuggestionCustom() {
        assertEquals(singletonMap("tcomp::action::built_in_suggestable", "alternate"), new ActionParameterEnricher()
                .onParameterAnnotation("testParam", String.class, new BuiltInSuggestable() {

                    @Override
                    public Name value() {
                        return Name.CUSTOM;
                    }

                    @Override
                    public String name() {
                        return "alternate";
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return BuiltInSuggestable.class;
                    }
                }));
    }

    @Test
    void suggestion() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::action::suggestions", "test");
                put("tcomp::action::suggestions::parameters", ".,foo,/bar/dummy");
            }
        }, new ActionParameterEnricher().onParameterAnnotation("testParam", String.class, new Suggestable() {

            @Override
            public String value() {
                return "test";
            }

            @Override
            public String[] parameters() {
                return new String[] { ".", "foo", "/bar/dummy" };
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Suggestable.class;
            }
        }));
    }

    @Test
    void condition() {
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
}
