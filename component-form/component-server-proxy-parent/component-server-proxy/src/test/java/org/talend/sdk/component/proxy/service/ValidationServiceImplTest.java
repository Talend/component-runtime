/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.service;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;

import org.junit.jupiter.api.RepeatedTest;
import org.talend.sdk.component.proxy.api.service.RequestContext;
import org.talend.sdk.component.proxy.api.service.ValidationService;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;

@CdiInject
@WithServer
class ValidationServiceImplTest {

    @Inject
    private ValidationService service;

    private final RequestContext context = new RequestContext() {

        @Override
        public String language() {
            return "en";
        }

        @Override
        public String findPlaceholder(final String attributeName) {
            return null;
        }

        @Override
        public Object attribute(final String key) {
            return null;
        }
    };

    @RepeatedTest(2) // to test caching
    void validateOk() throws ExecutionException, InterruptedException {
        final ValidationService.Result errors = service
                .validate(context, "dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseSNkYXRhc2V0I2RhdGFzZXQtMg",
                        Json.createObjectBuilder().build())
                .toCompletableFuture()
                .get();
        assertTrue(errors.getErrors().isEmpty());
    }

    @RepeatedTest(2) // to test caching
    void validateKo() throws ExecutionException, InterruptedException {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        final String id = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString("test-component#TheTestFamily#dataset#dataset-2".getBytes(StandardCharsets.UTF_8));
        final ValidationService.Result errors = service
                .validate(context, id,
                        factory.createObjectBuilder().add("configuration", factory.createObjectBuilder()).build())
                .toCompletableFuture()
                .get();
        assertEquals(singletonList(
                new ValidationService.ValidationError("/configuration", "connection is required and is not present")),
                errors.getErrors());
    }
}
