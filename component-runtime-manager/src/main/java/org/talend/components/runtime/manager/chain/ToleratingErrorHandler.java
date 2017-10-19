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
package org.talend.components.runtime.manager.chain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ToleratingErrorHandler implements ExecutionChain.ErrorHandler {

    private final int maxErrors;

    @Getter
    private int current;

    @Override
    public Object onError(final Object data, final RuntimeException exception) {
        if (maxErrors == current++) {
            throw exception;
        }
        return ExecutionChain.Skip.INSTANCE;
    }
}
