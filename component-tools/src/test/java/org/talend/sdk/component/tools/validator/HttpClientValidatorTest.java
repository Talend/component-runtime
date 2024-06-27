package org.talend.sdk.component.tools.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
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
                validator.validate(finder, Arrays.asList(ClientKoWrongExtends.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassDoNotExtendHttpClient() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClientKoNotExtendsAnything.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(ClientKoNotExtendsAnything.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassMethodMissingRequestAnnotation() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(WrongClientMissingOneRequest.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(WrongClientMissingOneRequest.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateClassMethodWithOtherAnnotation() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(WrongClientUsingOneWrongAnnotation.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(WrongClientUsingOneWrongAnnotation.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateWrongClientNoMethodRequest() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(WrongClientNoMethodRequest.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(WrongClientNoMethodRequest.class));
        assertEquals(2, errors.count());
    }

    @Test
    void validateClassOK() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClientCorrect.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(ClientCorrect.class));
        assertEquals(0, noerrors.count());
    }

    interface ClientCorrect extends HttpClient {

        @Request(method = "POST")
        String main1(String ok);
    }

    interface ClientKoWrongExtends extends List{

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
