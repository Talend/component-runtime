package org.talend.sdk.component.tools.validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;

public class SerializationValidator implements Validator{

    @Override
    public Stream<String> validate(AnnotationFinder finder, List<Class<?>> components) {
        return components.stream()
                .filter(this::isNotSerializable)
                .map((Class clazz) -> clazz + " is not Serializable")
                .sorted();
    }

    private boolean isNotSerializable(Class<?> clazz) {
        return !Serializable.class.isAssignableFrom(clazz);
    }
}
