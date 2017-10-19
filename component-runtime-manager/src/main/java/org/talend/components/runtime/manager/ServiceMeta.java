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
package org.talend.components.runtime.manager;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ServiceMeta {

    private final Object instance;

    private final Collection<ActionMeta> actions;

    @Data
    @RequiredArgsConstructor
    public static class ActionMeta {

        private final String family;

        private final String type;

        private final String action;

        private final Type[] arguments;

        private final List<ParameterMeta> parameters;

        private final Function<Map<String, String>, Object> invoker;
    }
}
