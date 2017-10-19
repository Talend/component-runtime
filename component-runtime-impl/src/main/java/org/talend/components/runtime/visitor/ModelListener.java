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
package org.talend.components.runtime
        .visitor;

import org.talend.component.api.input.Emitter;
import org.talend.component.api.input.PartitionMapper;
import org.talend.component.api.processor.Processor;

public interface ModelListener {

    default void onPartitionMapper(final Class<?> type, final PartitionMapper partitionMapper) {
        // no-op
    }

    default void onEmitter(final Class<?> type, final Emitter emitter) {
        // no-op
    }

    default void onProcessor(final Class<?> type, final Processor processor) {
        // no-op
    }
}
