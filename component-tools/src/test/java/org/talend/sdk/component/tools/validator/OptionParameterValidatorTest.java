/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.xbean.finder.AnnotationFinder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;

class OptionParameterValidatorTest {

    @Test
    void okMaxRecordsAndDuration() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start(@Option(Option.MAX_RECORDS_PARAMETER) long maxRecords,
                    @Option(Option.MAX_DURATION_PARAMETER) long maxDuration) {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        assertEquals(0L, result.count());
    }

    @Test
    void okMaxRecords() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start(@Option(Option.MAX_RECORDS_PARAMETER) long maxRecords) {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        assertEquals(0L, result.count());
    }

    @Test
    void okMaxDuration() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start(@Option(Option.MAX_DURATION_PARAMETER) long maxDuration) {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        assertEquals(0L, result.count());
    }

    @Test
    void okEmptyParameters() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start() {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        assertEquals(0L, result.count());
    }

    @Test
    void nokIncorrectOptionValue() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start(@Option("FOOO") long maxDuration) {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        final List<String> strings = result.collect(Collectors.toList());

        assertEquals(1, strings.size());
        assertEquals(
                "Option value on the parameter 'maxDuration' is not acceptable. Acceptable values: [maxDurationMs,maxRecords]",
                strings.get(0));
    }

    @Test
    void nokNotAnnotatedParameter() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start(long maxDuration) {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        final List<String> strings = result.collect(Collectors.toList());

        assertEquals(1, strings.size());
        assertEquals("Parameter 'maxDuration' should be either annotated with @Option or removed", strings.get(0));
    }

    @Test
    void nokWrongParameterType() {
        @Emitter
        class MaxRecordAndMaxDurationEmitter {

            @PostConstruct
            void start(@Option(Option.MAX_DURATION_PARAMETER) String maxDuration,
                    @Option(Option.MAX_RECORDS_PARAMETER) Object maxRecords) {
            }
        }

        final AnnotationFinder mockFinder = Mockito.mock(AnnotationFinder.class);
        Mockito.when(mockFinder.findAnnotatedMethods(PostConstruct.class))
                .thenReturn(Arrays.asList(MaxRecordAndMaxDurationEmitter.class.getDeclaredMethods()));
        Mockito.when(mockFinder.findAnnotatedMethods(Emitter.class))
                .thenReturn(Collections.emptyList());

        final OptionParameterValidator validator = new OptionParameterValidator();
        final Stream<String> result = validator.validate(mockFinder, Mockito.anyList());
        final List<String> strings = result.collect(Collectors.toList());

        assertEquals(2, strings.size());
        assertEquals("The 'maxDuration' parameter's type is not acceptable. Acceptable types: [Integer,Long,int,long]",
                strings.get(0));
        assertEquals("The 'maxRecords' parameter's type is not acceptable. Acceptable types: [Integer,Long,int,long]",
                strings.get(1));
    }
}