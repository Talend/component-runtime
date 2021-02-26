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
package org.talend.sdk.component.api.service.connection;

import org.talend.sdk.component.api.context.RuntimeContext;
import org.talend.sdk.component.api.exception.ComponentException;

import lombok.Data;
import lombok.NoArgsConstructor;

// as we can't pass the connection object to service as parameter auto, so we need this helper object to pass connection
// and close it
@Data
@NoArgsConstructor
public abstract class CloseConnectionObject {

    protected RuntimeContext runtimeContext;

    // implement it and can get connection object like this : (java.sql.Connection)(runtimeContext.getConnection());
    public abstract boolean close() throws ComponentException;

}
