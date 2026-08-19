/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.uispec.mapper.api.provider;

import java.util.Collection;
import java.util.function.Supplier;

import org.talend.sdk.component.form.model.uischema.UiSchema;

/**
 * Enable to initialize the titleMap of the UiSpec base don identifiers.
 */
public interface TitleMapProvider extends Supplier<Collection<UiSchema.NameValue>> {

    /**
     * @return the reference this provider handles, it is considered as an identifier.
     */
    String reference();

    /**
     * All providers are sorted with that integer and the first matching one is used.
     *
     * @return the ordinal of this provider.
     */
    default int ordinal() {
        return 0;
    }
}
