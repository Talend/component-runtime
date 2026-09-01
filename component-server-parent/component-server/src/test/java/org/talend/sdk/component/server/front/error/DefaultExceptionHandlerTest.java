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
package org.talend.sdk.component.server.front.error;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.service.FatalState;

class DefaultExceptionHandlerTest {

    private AutoCloseable closeable;

    @Mock
    private ComponentServerConfiguration configuration;

    @Mock
    private FatalState fatalState;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private DefaultExceptionHandler handler;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void virtualMachineErrorCauseOmitsExceptionMessage() {
        when(request.getRequestURI()).thenReturn("/api/v1/component/index");
        final OutOfMemoryError error = new OutOfMemoryError("some very large heap dump detail that must not leak");

        handler.toResponse(error);

        final ArgumentCaptor<String> causeCaptor = ArgumentCaptor.forClass(String.class);
        verify(fatalState).markFatal(causeCaptor.capture());
        final String cause = causeCaptor.getValue();
        assertTrue(cause.contains("OutOfMemoryError"));
        assertTrue(cause.contains("/api/v1/component/index"));
        assertFalse(cause.contains("some very large heap dump detail"));
    }

    @Test
    void virtualMachineErrorCauseHandlesUnknownRequest() {
        when(request.getRequestURI()).thenReturn(null);
        final StackOverflowError error = new StackOverflowError("deep recursion detail that must not leak");

        handler.toResponse(error);

        final ArgumentCaptor<String> causeCaptor = ArgumentCaptor.forClass(String.class);
        verify(fatalState).markFatal(causeCaptor.capture());
        final String cause = causeCaptor.getValue();
        assertTrue(cause.contains("StackOverflowError"));
        assertFalse(cause.contains("deep recursion detail"));
    }
}
