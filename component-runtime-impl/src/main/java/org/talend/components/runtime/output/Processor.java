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
package org.talend.components.runtime.output;

import org.talend.components.runtime.base.Lifecycle;

public interface Processor extends Lifecycle {

    void beforeGroup();

    // impl note: the output factory is mainly here for beam case, don't propagate it to the mainstream API
    //            since it will never work in the studio with current generation logic
    void afterGroup(OutputFactory output);

    void onNext(InputFactory input, OutputFactory output);
}
