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
package org.talend.sdk.component.api.context;

import org.talend.sdk.component.api.meta.Documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Documentation("If expect to use existed connection for mapper or processor in runtime, need to extend this. "
        + "Then get runtime connection object to call: this.getRuntimeContext().getConnection()."
        + " The functionality is for the Studio only.")
public class RuntimeContextInjector {

    // provide default runtime context object when runtime context is not injected
    // for example, this is used for studio only, studio will inject it auto, cloud platform will not inject it auto
    // avoid NPE if not inject
    private RuntimeContext runtimeContext = new RuntimeContext() {

        @Override
        public Object getConnection() {
            return null;
        }

    };

    // call the hidden set method by runtime platform to inject the runtimeContext object

    // call the hidden component runtime to get the runtime context, get the shared object
    // for example :
    // getRuntimeContext().getConnection();

}
