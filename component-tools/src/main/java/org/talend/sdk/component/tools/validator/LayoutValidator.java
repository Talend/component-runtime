/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.ARRAY;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.ENUM;
import static org.talend.sdk.component.runtime.manager.ParameterMeta.Type.OBJECT;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.tools.validator.Validators.ValidatorHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LayoutValidator implements Validator {

    private final Validators.ValidatorHelper helper;

    public LayoutValidator(final ValidatorHelper helper) {
        this.helper = helper;
    }

    @Override
    public Stream<String> validate(final AnnotationFinder finder, final List<Class<?>> components) {
        return components
                .stream()
                .map(helper::buildOrGetParameters)
                .flatMap(this::toFlatNonPrimitiveConfig)
                .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (p1, p2) -> p1))
                .entrySet()
                .stream()
                .flatMap(this::visitLayout);
    }

    private Stream<String> visitLayout(final Map.Entry<String, ParameterMeta> config) {

        final Set<String> fieldsInGridLayout = config
                .getValue()
                .getMetadata()
                .entrySet()
                .stream()
                .filter(meta -> meta.getKey().startsWith("tcomp::ui::gridlayout"))
                .flatMap(meta -> of(meta.getValue().split("\\|")))
                .flatMap(s -> of(s.split(",")))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        final Set<String> fieldsInOptionOrder = config
                .getValue()
                .getMetadata()
                .entrySet()
                .stream()
                .filter(meta -> meta.getKey().startsWith("tcomp::ui::optionsorder"))
                .flatMap(meta -> of(meta.getValue().split(",")))
                .collect(Collectors.toSet());

        if (fieldsInGridLayout.isEmpty() && fieldsInOptionOrder.isEmpty()) {
            return Stream.empty();
        }

        if (!fieldsInGridLayout.isEmpty() && !fieldsInOptionOrder.isEmpty()) {
            this.log.error("Concurrent layout found for '" + config.getKey() + "', the @OptionsOrder will be ignored.");
        }

        final Stream<String> errorsLib;
        if (!fieldsInGridLayout.isEmpty()) {
            errorsLib = fieldsInGridLayout
                    .stream()
                    .filter(fieldInLayout -> config
                            .getValue()
                            .getNestedParameters()
                            .stream()
                            .map(ParameterMeta::getName)
                            .noneMatch(field -> field.equals(fieldInLayout)))
                    .map(fieldInLayout -> "Option '" + fieldInLayout
                            + "' in @GridLayout doesn't exist in declaring class '" + config.getKey() + "'")
                    .sorted();

            config
                    .getValue()
                    .getNestedParameters()
                    .stream()
                    .filter(field -> !fieldsInGridLayout.contains(field.getName()))
                    .map(field -> "Field '" + field.getName() + "' in " + config.getKey()
                            + " is not declared in any layout.")
                    .forEach(this.log::error);
        } else {
            errorsLib = fieldsInOptionOrder
                    .stream()
                    .filter(fieldInLayout -> config
                            .getValue()
                            .getNestedParameters()
                            .stream()
                            .map(ParameterMeta::getName)
                            .noneMatch(field -> field.equals(fieldInLayout)))
                    .map(fieldInLayout -> "Option '" + fieldInLayout
                            + "' in @OptionOrder doesn't exist in declaring class '" + config.getKey() + "'")
                    .sorted();

            config
                    .getValue()
                    .getNestedParameters()
                    .stream()
                    .filter(field -> !fieldsInOptionOrder.contains(field.getName()))
                    .map(field -> "Field '" + field.getName() + "' in " + config.getKey()
                            + " is not declared in any layout.")
                    .forEach(this.log::error);
        }
        return errorsLib;
    }

    private Stream<AbstractMap.SimpleEntry<String, ParameterMeta>>
            toFlatNonPrimitiveConfig(final List<ParameterMeta> config) {
        if (config == null || config.isEmpty()) {
            return empty();
        }
        return config
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> OBJECT.equals(p.getType()) || isArrayOfObject(p))
                .filter(p -> p.getNestedParameters() != null)
                .flatMap(p -> concat(of(new AbstractMap.SimpleEntry<>(toJavaType(p).getName(), p)),
                        toFlatNonPrimitiveConfig(p.getNestedParameters())));
    }

    private boolean isArrayOfObject(final ParameterMeta param) {

        return ARRAY.equals(param.getType()) && param.getNestedParameters() != null
                && param
                        .getNestedParameters()
                        .stream()
                        .anyMatch(p -> OBJECT.equals(p.getType()) || ENUM.equals(p.getType()) || isArrayOfObject(p));

    }

    private Class<?> toJavaType(final ParameterMeta p) {
        if (p.getType().equals(OBJECT) || p.getType().equals(ENUM)) {
            if (Class.class.isInstance(p.getJavaType())) {
                return Class.class.cast(p.getJavaType());
            }
            throw new IllegalArgumentException("Unsupported type for parameter " + p.getPath() + " (from "
                    + p.getSource().declaringClass() + "), ensure it is a Class<?>");
        }

        if (p.getType().equals(ARRAY) && ParameterizedType.class.isInstance(p.getJavaType())) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(p.getJavaType());
            final Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length == 1 && Class.class.isInstance(arguments[0])) {
                return Class.class.cast(arguments[0]);
            }
            throw new IllegalArgumentException("Unsupported type for parameter " + p.getPath() + " (from "
                    + p.getSource().declaringClass() + "), " + "ensure it is a ParameterizedType with one argument");
        }

        throw new IllegalStateException("Parameter '" + p.getName() + "' is not an object.");
    }
}
