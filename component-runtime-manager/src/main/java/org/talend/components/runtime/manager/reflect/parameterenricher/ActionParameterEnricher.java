// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.component.api.configuration.action.Discoverable;
import org.talend.component.api.configuration.action.meta.ActionRef;
import org.talend.component.api.service.ActionType;
import org.talend.components.spi.parameter.ParameterExtensionEnricher;

public class ActionParameterEnricher implements ParameterExtensionEnricher {

    public static final String META_PREFIX = "tcomp::action::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final ActionRef ref = annotation.annotationType().getAnnotation(ActionRef.class);
        if (ref == null) {
            return emptyMap();
        }
        final String type = ref.value().getAnnotation(ActionType.class).value();
        return new HashMap<String, String>() {

            {
                put(META_PREFIX + type, getValueString(annotation));
                ofNullable(getParametersString(annotation)).ifPresent(v -> put(META_PREFIX + type + "::parameters", v));
                ofNullable(getBinding(annotation)).ifPresent(v -> put(META_PREFIX + type + "::binding", v));
            }
        };
    }

    private String getValueString(final Annotation annotation) {
        try {
            return String.valueOf(annotation.annotationType().getMethod("value").invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("No value for " + annotation);
        }
    }

    private String getBinding(final Annotation annotation) {
        try {
            return Discoverable.Binding.class.cast(annotation.annotationType().getMethod("binding").invoke(annotation)).name();
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    private String getParametersString(final Annotation annotation) {
        try {
            return Stream.of(String[].class.cast(annotation.annotationType().getMethod("parameters").invoke(annotation)))
                    .collect(Collectors.joining(","));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
