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
package org.talend.test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.Producer;

@Emitter(family = "chain", name = "list")
public class ListInput implements Serializable {

    private final Collection<String> list;

    private transient Iterator<String> iterator;

    public ListInput(@Option("values") final List<String> list) {
        this.list = list;
    }

    @Producer
    public String data() {
        if (iterator == null) {
            iterator = list.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
