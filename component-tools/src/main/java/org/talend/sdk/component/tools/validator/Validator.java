package org.talend.sdk.component.tools.validator;

import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;

public interface Validator {

    Stream<String> validate(AnnotationFinder finder, List<Class<?>> components);
}
