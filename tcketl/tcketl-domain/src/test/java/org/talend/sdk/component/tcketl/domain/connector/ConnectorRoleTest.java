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
package org.talend.sdk.component.tcketl.domain.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConnectorRoleTest {

    @ParameterizedTest(name = "inputs={0} outputs={1} gives {2}")
    @MethodSource("cases")
    void derivesTheRoleFromTheFlows(final List<String> inputs, final List<String> outputs,
            final ConnectorRole expected) {
        assertEquals(expected, ConnectorRole.fromFlows(inputs, outputs));
    }

    static Stream<Arguments> cases() {
        return Stream
                .of(Arguments.of(List.of(), List.of("__default__"), ConnectorRole.INPUT),
                        Arguments
                                .of(List.of("__default__"), List.of("__default__", "REJECT"),
                                        ConnectorRole.PROCESSOR),
                        Arguments.of(List.of("__default__"), List.of(), ConnectorRole.OUTPUT),
                        Arguments.of(List.of(), List.of(), ConnectorRole.STANDALONE),
                        Arguments.of(List.of("__default__"), List.of("REJECT"), ConnectorRole.PROCESSOR));
    }
}
