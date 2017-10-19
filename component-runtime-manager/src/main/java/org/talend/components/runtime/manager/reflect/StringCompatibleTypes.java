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

import java.lang.reflect.Type;

import org.apache.xbean.propertyeditor.PropertyEditors;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class StringCompatibleTypes {

    static boolean isKnown(final Type type) {
        return String.class == type || char.class == type || Character.class == type
                || (Class.class.isInstance(type) && PropertyEditors.canConvert(Class.class.cast(type)));
    }
}
