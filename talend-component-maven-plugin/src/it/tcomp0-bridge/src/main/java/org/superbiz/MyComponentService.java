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

import static java.util.Collections.singletonList;

import org.talend.component.api.service.Service;
import org.talend.component.api.service.schema.DiscoverSchema;
import org.talend.component.api.service.schema.Schema;
import org.talend.component.api.service.schema.Type;

@Service
public class MyComponentService {
    @DiscoverSchema(family = "test")
    public Schema defineSchema() { // this component hardcodes it, default guess logic should work too
        return new Schema(singletonList(new Schema.Entry("value", Type.STRING)));
    }
}
