/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.inject.Vetoed;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.extension.stitch.model.Model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// todo: extract that retry client somewhere or isn't it worth it?
@Slf4j
@Vetoed
@RequiredArgsConstructor
class ModelLoader {

    private final WebTarget target;

    private final String token;

    private final int retries;

    CompletionStage<Model> load() {
        final CompletionStageRxInvoker invoker =
                target.path("model").request(APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, token).rx();
        return invoker.get().handle((ok, error) -> {
            if (isSuccess(ok, error)) {
                return completedFuture(ok);
            }
            return doRetry(getError(ok, error), 0, invoker);
        }).thenCompose(identity()).thenApply(response -> response.readEntity(Model.class));
    }

    private boolean isSuccess(final Response ok, final Throwable error) {
        return error == null && ok.getStatus() >= 200 && ok.getStatus() < 300;
    }

    private Throwable getError(final Response ok, final Throwable error) {
        return error == null ? new IllegalArgumentException("Response HTTP " + ok.getStatus()) : error;
    }

    private CompletionStage<Response> doRetry(final Throwable originalFailure, final int retry,
            final CompletionStageRxInvoker invoker) {
        if (retry == retries) { // we done
            final CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(originalFailure);
            return future;
        }
        log.debug("Retrying request....");
        return invoker.get().handle((ok, error) -> {
            if (isSuccess(ok, error)) {
                return completedFuture(ok);
            }
            originalFailure.addSuppressed(getError(ok, error));
            try { // this one is mainly for test, don't expose it more than that
                sleep(Integer.getInteger("talend.server.extension.stitch.client.retry.timeout", 1000));
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return doRetry(originalFailure, retry + 1, invoker);
        }).thenCompose(identity());
    }
}
