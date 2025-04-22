/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.intellij.module;

import static org.talend.sdk.component.intellij.Configuration.getMessage;

import javax.swing.Icon;

import com.intellij.openapi.module.ModuleType;

import org.talend.sdk.component.intellij.Icons;

public class TalendModuleType extends ModuleType<TalendModuleBuilder> {

    public TalendModuleType() {
        super("TALEND_COMPONENT_MODULE");
    }

    @Override
    public TalendModuleBuilder createModuleBuilder() {
        return new TalendModuleBuilder(this);
    }

    @Override
    public String getName() {
        return getMessage("plugin.name");
    }

    @Override
    public String getDescription() {
        return getMessage("plugin.description");
    }

    @Override
    public Icon getNodeIcon(final boolean b) {
        return Icons.TACOKIT.getIcon();
    }

    @Override
    public Icon getIcon() {
        return Icons.TACOKIT.getIcon();
    }
}
