package org.talend.sdk.component.tools.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Request;

public class HttpClientValidatorTest {
    @Test
    void validateErrors1() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(MethodKo.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(MethodKo.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateErrors2() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(ClientKo.class));
        final Stream<String> errors =
                validator.validate(finder, Arrays.asList(ClientKo.class));
        assertEquals(1, errors.count());
    }

    @Test
    void validateOKs() {
        final HttpValidator validator = new HttpValidator();
        AnnotationFinder finder = new AnnotationFinder(new ClassesArchive(SimpleClient.class));
        final Stream<String> noerrors =
                validator.validate(finder, Arrays.asList(SimpleClient.class));
        assertEquals(0, noerrors.count());
    }

    public interface SimpleClient extends HttpClient {

        @Request(method = "POST")
        String main1(String ok);
    }

    public interface MethodKo extends List{

        @Request
        List<Object> main(String payload);
    }


    public interface ClientKo {

        @Request
        String main();
    }

}
