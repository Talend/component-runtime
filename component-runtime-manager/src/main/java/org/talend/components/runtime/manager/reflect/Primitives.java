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
package org.talend.components.runtime.manager.reflect;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = PRIVATE)
public final class Primitives {

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVES = new HashMap<Class<?>, Class<?>>() {

        {
            put(Integer.class, int.class);
            put(Short.class, short.class);
            put(Byte.class, byte.class);
            put(Character.class, char.class);
            put(Long.class, long.class);
            put(Float.class, float.class);
            put(Double.class, double.class);
            put(Boolean.class, boolean.class);
        }
    };

    public static Class<?> unwrap(final Class<?> type) {
        return WRAPPER_TO_PRIMITIVES.getOrDefault(type, type);
    }
}
