/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.front.error;

import java.io.Serializable;
import java.util.concurrent.CompletionStage;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.talend.sdk.component.proxy.service.ErrorProcessor;

@Interceptor
@AutoErrorHandling(single = false)
@Priority(Interceptor.Priority.LIBRARY_BEFORE - 1)
public class MultipleErrorHandlerInterceptor implements Serializable {

    @Inject
    private ErrorProcessor errorProcessor;

    @AroundInvoke
    public Object handle(final InvocationContext context) throws Exception {
        return errorProcessor.handleResponses(CompletionStage.class.cast(context.proceed()));
    }
}
