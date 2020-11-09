package org.talend.sdk.component.tools.validator;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.Documentation;

public class DocumentationValidator implements Validator {

    @Override
    public Stream<String> validate(AnnotationFinder finder, List<Class<?>> components) {
        return Stream.concat(components
                        .stream()
                        .filter(c -> !c.isAnnotationPresent(Documentation.class))
                        .map((Class<?> c) -> "No @Documentation on '" + c.getName() + "'")
                        .sorted(),
                finder.findAnnotatedFields(Option.class)
                        .stream()
                        .filter(field -> !field.isAnnotationPresent(Documentation.class)
                                && !field.getType().isAnnotationPresent(Documentation.class))
                        .map(field -> "No @Documentation on '" + field + "'")
                        .sorted());
    }
}
