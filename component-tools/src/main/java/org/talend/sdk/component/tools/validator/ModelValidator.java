package org.talend.sdk.component.tools.validator;

import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class ModelValidator implements Validator {

    private final boolean validateComponent;

    private final ValidatorHelper helper;

    public ModelValidator(boolean validateComponent, ValidatorHelper helper) {
        this.validateComponent = validateComponent;
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(AnnotationFinder finder, List<Class<?>> components) {
        final Stream<String> errorsAnnotations = components.stream()
                .filter(this::containsIncompatibleAnnotation)
                .map(i -> i + " has conflicting component annotations, ensure it has a single one")
                .sorted();

        final Stream<String> errorsParamConstructors = components.stream()
                .filter(c -> countParameters(findConstructor(c).getParameters()) > 1)
                .map(c -> "Component must use a single root option. '" + c.getName() + "'")
                .sorted();

        final ModelVisitor modelVisitor = new ModelVisitor();
        final ModelListener noop = new ModelListener() {  };

        final Stream<String> errorsConfig = components.stream().map(c -> {
            try {
                modelVisitor.visit(c, noop, this.validateComponent);
                return null;
            } catch (final RuntimeException re) {
                return re.getMessage();
            }
        }).filter(Objects::nonNull)
                .sorted();

        // limited config types
        final Stream<String> errorStructure = finder.findAnnotatedFields(Structure.class)
                .stream()
                .filter(f -> !ParameterizedType.class.isInstance(f.getGenericType()) || (!isListString(f)
                        && !isMapString(f)))
                .map(f -> f.getDeclaringClass() + "#" + f.getName()
                        + " uses @Structure but is not a List<String> nor a Map<String, String>")
                .sorted();

        return Stream.of(errorsAnnotations, errorsParamConstructors, errorsConfig, errorStructure)
                .reduce(Stream::concat).orElse(Stream.empty());
    }

    private boolean containsIncompatibleAnnotation(final Class<?> clazz) {
        return Stream.of(PartitionMapper.class, Processor.class, Emitter.class)
                .filter((Class<? extends Annotation> an) -> clazz.isAnnotationPresent(an))
                .count() > 1;

    }

    private int countParameters(final Parameter[] params) {
        return (int) Stream
                .of(params)
                .filter((Parameter p) -> !this.helper.isService(p))
                .count();
    }

    private boolean isMapString(final Field f) {
        final ParameterizedType pt = ParameterizedType.class.cast(f.getGenericType());
        return (Map.class == pt.getRawType()) && pt.getActualTypeArguments().length == 2
                && pt.getActualTypeArguments()[0] == String.class && pt.getActualTypeArguments()[1] == String.class;
    }

    private boolean isListString(final Field f) {
        final ParameterizedType pt = ParameterizedType.class.cast(f.getGenericType());
        return ((List.class == pt.getRawType()) || (Collection.class == pt.getRawType()))
                && pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0] == String.class;
    }
}
