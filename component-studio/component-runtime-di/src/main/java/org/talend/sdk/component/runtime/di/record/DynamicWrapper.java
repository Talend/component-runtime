package org.talend.sdk.component.runtime.di.record;

import lombok.Getter;
import routines.system.Dynamic;

@Getter
class DynamicWrapper {

    private final Dynamic dynamic;

    DynamicWrapper() {
        dynamic = new Dynamic();
    }

}
