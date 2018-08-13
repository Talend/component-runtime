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
package org.talend.sdk.component.proxy.service.client;

import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.InvocationCallback;

import lombok.RequiredArgsConstructor;

/**
 * Client need to use a concrete instance with a concrete type otherwise cxf can't get the response type..
 *
 * @param <T> : Response type
 */
@RequiredArgsConstructor
public abstract class RxInvocationCallback<T> implements InvocationCallback<T> {

    private final CompletableFuture<T> completableFuture;

    @Override
    public void completed(final T response) {
        completableFuture.complete(response);
    }

    @Override
    public void failed(final Throwable throwable) {
        completableFuture.completeExceptionally(throwable);
    }
}
