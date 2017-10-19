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
package org.superbiz;

import static org.talend.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;

import org.talend.component.api.component.Icon;
import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

@Icon(FILE_JOB_O)
@Processor(family = "test", name = "my")
public class MyComponent implements Serializable {

    @ElementListener
    public Foo passthrough(final Foo item) {
        return item;
    }

    public static class Foo {
    }
}
