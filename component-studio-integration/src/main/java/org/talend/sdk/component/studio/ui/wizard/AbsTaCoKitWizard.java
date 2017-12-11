/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.sdk.component.studio.ui.wizard;

import org.eclipse.ui.IWorkbench;
import org.talend.metadata.managment.ui.wizard.CheckLastVersionRepositoryWizard;

public abstract class AbsTaCoKitWizard extends CheckLastVersionRepositoryWizard {

    private TaCoKitConfigurationRuntimeData runtimeData;

    public AbsTaCoKitWizard(final IWorkbench workbench, final TaCoKitConfigurationRuntimeData runtimeData) {
        super(workbench, runtimeData.isCreation(), runtimeData.isReadonly());
        this.runtimeData = runtimeData;
        init();
    }

    protected void init() {
        this.existingNames = getTaCoKitConfigurationRuntimeData().getExistingNames();
    }

    protected TaCoKitConfigurationRuntimeData getTaCoKitConfigurationRuntimeData() {
        return this.runtimeData;
    }

}
