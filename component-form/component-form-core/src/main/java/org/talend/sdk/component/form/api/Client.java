/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.api;

import java.util.Map;
import java.util.concurrent.CompletionStage;

// note: we can make it async since both impl support it but does it make much sense OOTB?

/**
 * Abstract the HTTP layer. Note that the implementation must support a
 * constructor with a String parameter representing the base of the http
 * requests.
 */
public interface Client<T> extends AutoCloseable {

    CompletionStage<Map<String, Object>> action(String family, String type, String action, String lang,
            Map<String, Object> params, T context);

    @Override
    void close();
}
