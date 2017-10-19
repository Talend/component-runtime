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
import static java.util.Collections.singletonMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;

import org.talend.component.api.configuration.type.meta.ConfigurationType;
import org.talend.components.spi.parameter.ParameterExtensionEnricher;

public class ConfigurationTypeParameterEnricher implements ParameterExtensionEnricher {

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final ConfigurationType configType = annotation.annotationType().getAnnotation(ConfigurationType.class);
        if (configType != null) {
            final String type = configType.value();
            final String name = getName(annotation);
            if (name != null) {
                return singletonMap(/* not prefixed by tcomp:: since it is a core/direct metadata */type, name);
            }
        }
        return emptyMap();
    }

    private String getName(final Annotation annotation) {
        try {
            return String.class.cast(annotation.annotationType().getMethod("value").invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
