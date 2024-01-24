/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

public class ModelValidator implements Validator {

    private final boolean validateComponent;

    private final ValidatorHelper helper;

    public ModelValidator(final boolean validateComponent, final ValidatorHelper helper) {
        this.validateComponent = validateComponent;
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        final Stream<String> errorsAnnotations = components
                .stream()
                .filter(this::containsIncompatibleAnnotation)
                .map(i -> i + " has conflicting component annotations, ensure it has a single one")
                .sorted();

        final Stream<String> errorsParamConstructors = components
                .stream()
                .filter(c -> countParameters(findConstructor(c).getParameters()) > 1)
                .map(c -> "Component must use a single root option. '" + c.getName() + "'")
                .sorted();

        final ModelVisitor modelVisitor = new ModelVisitor();
        final ModelListener noop = new ModelListener() {
        };

        final Stream<String> errorsConfig = components.stream().map(c -> {
            try {
                modelVisitor.visit(c, noop, this.validateComponent);
                return null;
            } catch (final RuntimeException re) {
                return re.getMessage();
            }
        }).filter(Objects::nonNull).sorted();

        // limited config types
        final Stream<String> errorStructure = finder
                .findAnnotatedFields(Structure.class)
                .stream()
                .filter(f -> !ParameterizedType.class.isInstance(f.getGenericType()) || !isListObject(f))
                .map(f -> f.getDeclaringClass() + "#" + f.getName()
                        + " uses @Structure but is not a List<String> nor a List<Object>")
                .sorted();

        return Stream
                .of(errorsAnnotations, errorsParamConstructors, errorsConfig, errorStructure)
                .reduce(Stream::concat)
                .orElse(Stream.empty());
    }

    private boolean containsIncompatibleAnnotation(final Class<?> clazz) {
        return Stream
                .of(PartitionMapper.class, Processor.class, Emitter.class, DriverRunner.class)
                .filter((Class<? extends Annotation> an) -> clazz.isAnnotationPresent(an))
                .count() > 1;

    }

    private int countParameters(final Parameter[] params) {
        return (int) Stream.of(params).filter((Parameter p) -> !this.helper.isService(p)).count();
    }

    private boolean isListObject(final Field f) {
        final ParameterizedType pt = ParameterizedType.class.cast(f.getGenericType());
        return List.class == pt.getRawType() && pt.getActualTypeArguments().length == 1;
    }

}
