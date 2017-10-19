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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ContainerComponentRegistry {

    private final Map<String, ComponentFamilyMeta> components = new ConcurrentHashMap<>();

    private final Collection<ServiceMeta> services = new ArrayList<>();
}
