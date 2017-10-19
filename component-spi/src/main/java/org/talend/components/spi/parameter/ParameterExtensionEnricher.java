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
package org.talend.components.spi.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Allow to build extensions of an object parameter.
 */
public interface ParameterExtensionEnricher {

    /**
     * Visit all annotations of an object parameter and return for each annotation
     * the related extensions. Note it is highly recommanded even if not enforced
     * to use a prefix by extension type.
     *
     * @param parameterName the name of the parameter currently visited.
     * @param annotation the currently visited annotation.
     * @return the extensions to add for this parameter.
     */
    Map<String, String> onParameterAnnotation(String parameterName, Type parameterType, Annotation annotation);
}
