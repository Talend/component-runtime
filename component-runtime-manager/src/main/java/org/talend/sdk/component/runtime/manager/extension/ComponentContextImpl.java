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
package org.talend.sdk.component.runtime.manager.extension;

import static lombok.AccessLevel.NONE;

import org.talend.sdk.component.spi.component.ComponentExtension;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ComponentContextImpl implements ComponentExtension.ComponentContext {

    private final Class<?> type;

    private boolean noValidation;

    @Setter(NONE)
    private ComponentExtension owningExtension;

    @Getter(NONE)
    private ComponentExtension currentExtension;

    @Override
    public void skipValidation() {
        if (!noValidation) {
            owningExtension = currentExtension;
            noValidation = true;
        }
    }

    @Override
    public ComponentExtension owningExtension() {
        return owningExtension;
    }
}
