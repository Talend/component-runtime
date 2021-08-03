/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.talend.sdk.component.api.service.serialization.Serial;

import lombok.Data;

/**
 * Classe base for services to be declared Serializable and use Component Manager serialization system.
 */
@Data
public class BaseService implements Serializable {

    private static final long serialVersionUID = -5486791400889992135L;

    private Serial serializationHelper;

    protected final Object writeReplace() throws ObjectStreamException {
        if (this.serializationHelper != null) {
            return this.serializationHelper;
        }
        throw new IllegalArgumentException("Serialization not found for " + this.getClass().getName());
    }
}
