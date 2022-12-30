package org.talend.sdk.component.runtime.di.record;

import lombok.Getter;
import routines.system.DynamicMetadata;

@Getter
class DynamicMetadataWrapper {

    private DynamicMetadata dynamicMetadata;

    DynamicMetadataWrapper() {
        dynamicMetadata = new DynamicMetadata();
    }

    DynamicMetadataWrapper(final DynamicMetadata dynamicMetadata) {
        this.dynamicMetadata = dynamicMetadata;
    }

    void initSourceType() {
        dynamicMetadata.setSourceType(DynamicMetadata.sourceTypes.unknown);
    }

}
