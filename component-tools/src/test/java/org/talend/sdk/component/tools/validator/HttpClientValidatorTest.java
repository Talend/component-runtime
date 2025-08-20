/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Request;

class HttpClientValidatorTest {

    @Test
    void validateClassExtendWrongCLass() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClientKoWrongExtends.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(ClientKoWrongExtends.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassDoNotExtendHttpClient() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClientKoNotExtendsAnything.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(ClientKoNotExtendsAnything.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassMethodMissingRequestAnnotation() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(WrongClientMissingOneRequest.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(WrongClientMissingOneRequest.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassMethodWithOtherAnnotation() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(WrongClientUsingOneWrongAnnotation.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(WrongClientUsingOneWrongAnnotation.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateWrongClientNoMethodRequest() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(WrongClientNoMethodRequest.class));
        final Stream<String> errors =
                validator.validate(finder, Collections.singletonList(WrongClientNoMethodRequest.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassOK() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClientCorrect.class));
        final Stream<String> noerrors =
                validator.validate(finder, Collections.singletonList(ClientCorrect.class));
        assertEquals(0, noerrors.count());
    }

    interface ClientCorrect extends HttpClient {

        @Request(method = "POST")
        String main1(String ok);
    }

    interface ClientKoWrongExtends extends List {

        @Request
        List<Object> main(String payload);
    }

    interface ClientKoNotExtendsAnything {

        @Request
        String main();
    }

    interface WrongClientMissingOneRequest extends HttpClient {

        // It misses @Request
        String queryA(String ok);

        @Request(method = "POST")
        String queryB(String ok);
    }

    interface WrongClientUsingOneWrongAnnotation extends HttpClient {

        // It misses @Request
        @DynamicDependencies
        String queryA(String ok);

        @Request(method = "POST")
        String queryB(String ok);
    }

    interface WrongClientNoMethodRequest extends HttpClient {

        // It misses @Request
        String queryA(String ok);

        // It misses @Request
        String queryB(String ok);
    }
}
