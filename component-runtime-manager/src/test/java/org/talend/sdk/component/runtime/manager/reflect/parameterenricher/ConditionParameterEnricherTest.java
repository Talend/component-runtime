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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.condition.UIScope;

class ConditionParameterEnricherTest {

    @Test
    void multipleConditions() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::ifs::operator", "AND");

                put("tcomp::condition::if::target::0", "foo.bar");
                put("tcomp::condition::if::value::0", "true,false");
                put("tcomp::condition::if::negate::0", "false");
                put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");

                put("tcomp::condition::if::target::1", "dummy");
                put("tcomp::condition::if::value::1", "ok");
                put("tcomp::condition::if::negate::1", "false");
                put("tcomp::condition::if::evaluationStrategy::1", "DEFAULT");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIfs() {

            @Override
            public Operator operator() {
                return Operator.AND;
            }

            @Override
            public ActiveIf[] value() {
                return new ActiveIf[] { new ActiveIf() {

                    @Override
                    public EvaluationStrategyOption[] evaluationStrategyOptions() {
                        return new EvaluationStrategyOption[0];
                    }

                    @Override
                    public String target() {
                        return "foo.bar";
                    }

                    @Override
                    public String[] value() {
                        return new String[] { "true", "false" };
                    }

                    @Override
                    public boolean negate() {
                        return false;
                    }

                    @Override
                    public EvaluationStrategy evaluationStrategy() {
                        return EvaluationStrategy.DEFAULT;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return ActiveIf.class;
                    }
                }, new ActiveIf() {

                    @Override
                    public EvaluationStrategyOption[] evaluationStrategyOptions() {
                        return new EvaluationStrategyOption[0];
                    }

                    @Override
                    public String target() {
                        return "dummy";
                    }

                    @Override
                    public String[] value() {
                        return new String[] { "ok" };
                    }

                    @Override
                    public boolean negate() {
                        return false;
                    }

                    @Override
                    public EvaluationStrategy evaluationStrategy() {
                        return EvaluationStrategy.DEFAULT;
                    }

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return ActiveIf.class;
                    }
                } };
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIfs.class;
            }
        }));
    }

    @Test
    void condition() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::if::target", "foo.bar");
                put("tcomp::condition::if::value", "true");
                put("tcomp::condition::if::negate", "false");
                put("tcomp::condition::if::evaluationStrategy", "DEFAULT");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIf() {

            @Override
            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                return new EvaluationStrategyOption[0];
            }

            @Override
            public String target() {
                return "foo.bar";
            }

            @Override
            public String[] value() {
                return new String[] { "true" };
            }

            @Override
            public boolean negate() {
                return false;
            }

            @Override
            public EvaluationStrategy evaluationStrategy() {
                return EvaluationStrategy.DEFAULT;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIf.class;
            }
        }));
    }

    @Test
    void conditionListValues() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::if::target", "foo.bar");
                put("tcomp::condition::if::value", "true,false");
                put("tcomp::condition::if::negate", "false");
                put("tcomp::condition::if::evaluationStrategy", "DEFAULT");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIf() {

            @Override
            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                return new EvaluationStrategyOption[0];
            }

            @Override
            public String target() {
                return "foo.bar";
            }

            @Override
            public String[] value() {
                return new String[] { "true", "false" };
            }

            @Override
            public boolean negate() {
                return false;
            }

            @Override
            public EvaluationStrategy evaluationStrategy() {
                return EvaluationStrategy.DEFAULT;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIf.class;
            }
        }));
    }

    @Test
    void conditionWithConfiguredStrategy() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::if::target", "foo.bar");
                put("tcomp::condition::if::value", "true,false");
                put("tcomp::condition::if::negate", "false");
                put("tcomp::condition::if::evaluationStrategy", "CONTAINS(lowercase=true)");
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
            public boolean negate() {
                return false;
            }

            @Override
            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                return new EvaluationStrategyOption[] { new EvaluationStrategyOption() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return EvaluationStrategyOption.class;
                    }

                    @Override
                    public String name() {
                        return "lowercase";
                    }

                    @Override
                    public String value() {
                        return "true";
                    }
                } };
            }

            @Override
            public EvaluationStrategy evaluationStrategy() {
                return EvaluationStrategy.CONTAINS;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIf.class;
            }
        }));
    }

    @Test
    void conditionWithUIScope() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::if::target", UIScope.TARGET);
                put("tcomp::condition::if::value",
                        UIScope.CLOUD_SCOPE + "," + UIScope.STUDIO_SCOPE);
                put("tcomp::condition::if::negate", "false");
                put("tcomp::condition::if::evaluationStrategy", "DEFAULT");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIf() {

            @Override
            public String target() {
                return UIScope.TARGET;
            }

            @Override
            public String[] value() {
                return new String[] { UIScope.CLOUD_SCOPE, UIScope.STUDIO_SCOPE };
            }

            @Override
            public boolean negate() {
                return false;
            }

            @Override
            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                return new EvaluationStrategyOption[0];
            }

            @Override
            public EvaluationStrategy evaluationStrategy() {
                return EvaluationStrategy.DEFAULT;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIf.class;
            }
        }));
    }

    @Test
    void activeIfsOnSameTarget() {
        assertEquals(new HashMap<String, String>() {

            {
                put("tcomp::condition::ifs::operator", "AND");
                put("tcomp::condition::if::target::0", "filter.operator");
                put("tcomp::condition::if::value::0", "IS_NULL");
                put("tcomp::condition::if::negate::0", "true");
                put("tcomp::condition::if::evaluationStrategy::0", "DEFAULT");
                put("tcomp::condition::if::target::1", "filter.operator");
                put("tcomp::condition::if::value::1", "IS_NOT_NULL");
                put("tcomp::condition::if::negate::1", "true");
                put("tcomp::condition::if::evaluationStrategy::1", "DEFAULT");
                put("tcomp::condition::if::target::2", "filter.operator");
                put("tcomp::condition::if::value::2", "IS_EMPTY");
                put("tcomp::condition::if::negate::2", "true");
                put("tcomp::condition::if::evaluationStrategy::2", "DEFAULT");
                put("tcomp::condition::if::target::3", "filter.operator");
                put("tcomp::condition::if::value::3", "IS_NOT_EMPTY");
                put("tcomp::condition::if::negate::3", "true");
                put("tcomp::condition::if::evaluationStrategy::3", "DEFAULT");
            }
        }, new ConditionParameterEnricher().onParameterAnnotation("testParam", String.class, new ActiveIfs() {

            @Override
            public Operator operator() {
                return Operator.AND;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ActiveIfs.class;
            }

            @Override
            public ActiveIf[] value() {
                return new ActiveIf[] {
                        new ActiveIf() {

                            @Override
                            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                                return new EvaluationStrategyOption[0];
                            }

                            @Override
                            public String target() {
                                return "filter.operator";
                            }

                            @Override
                            public boolean negate() {
                                return true;
                            }

                            @Override
                            public EvaluationStrategy evaluationStrategy() {
                                return EvaluationStrategy.DEFAULT;
                            }

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return ActiveIf.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "IS_NULL" };
                            }
                        },
                        new ActiveIf() {

                            @Override
                            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                                return new EvaluationStrategyOption[0];
                            }

                            @Override
                            public String target() {
                                return "filter.operator";
                            }

                            @Override
                            public boolean negate() {
                                return true;
                            }

                            @Override
                            public EvaluationStrategy evaluationStrategy() {
                                return EvaluationStrategy.DEFAULT;
                            }

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return ActiveIf.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "IS_NOT_NULL" };
                            }
                        },
                        new ActiveIf() {

                            @Override
                            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                                return new EvaluationStrategyOption[0];
                            }

                            @Override
                            public String target() {
                                return "filter.operator";
                            }

                            @Override
                            public boolean negate() {
                                return true;
                            }

                            @Override
                            public EvaluationStrategy evaluationStrategy() {
                                return EvaluationStrategy.DEFAULT;
                            }

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return ActiveIf.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "IS_EMPTY" };
                            }
                        },
                        new ActiveIf() {

                            @Override
                            public EvaluationStrategyOption[] evaluationStrategyOptions() {
                                return new EvaluationStrategyOption[0];
                            }

                            @Override
                            public String target() {
                                return "filter.operator";
                            }

                            @Override
                            public boolean negate() {
                                return true;
                            }

                            @Override
                            public EvaluationStrategy evaluationStrategy() {
                                return EvaluationStrategy.DEFAULT;
                            }

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return ActiveIf.class;
                            }

                            @Override
                            public String[] value() {
                                return new String[] { "IS_NOT_EMPTY" };
                            }
                        }
                };
            }
        }));
    }
}
