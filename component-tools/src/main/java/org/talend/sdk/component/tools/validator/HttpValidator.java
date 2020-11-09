package org.talend.sdk.component.tools.validator;

import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;

public class HttpValidator implements Validator {

    @Override
    public Stream<String> validate(AnnotationFinder finder, List<Class<?>> components) {
        return finder
                .findAnnotatedClasses(Request.class) //
                .stream() //
                .map(Class::getDeclaringClass) //
                .distinct() //
                .flatMap(c -> HttpClientFactoryImpl.createErrors(c).stream()) //
                .sorted();
    }
}
