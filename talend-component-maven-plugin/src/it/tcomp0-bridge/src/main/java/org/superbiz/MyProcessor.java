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

import static org.talend.component.api.component.Icon.IconType.EYE;

import java.io.Serializable;

import org.talend.component.api.component.Icon;
import org.talend.component.api.component.Version;
import org.talend.component.api.configuration.Option;
import org.talend.component.api.processor.ElementListener;
import org.talend.component.api.processor.Processor;

@Version
@Icon(EYE)
@Processor(family = "test", name = "my")
public class MyProcessor implements Serializable {
    private boolean log;

    public MyProcessor(@Option("configuration") final Configuration configuration) {
        this.log = configuration.log == null || configuration.log;
    }

    @ElementListener
    public Foo passthrough(final Foo item) {
        if (log) {
            System.out.println(getClass().getSimpleName() + "> " + item);
        }
        return item;
    }

    public static class Foo {
        private String date;

        public String getDate() {
            return date;
        }
    }

    public static class Configuration {
        @Option
        private Boolean log;
    }
}
