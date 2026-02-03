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
package org.talend.sdk.component.server.service.jcache;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class FrontCacheKeyGenerator implements CacheKeyGenerator {

    @Inject
    @Context
    private Request request;

    @Inject
    @Context
    private UriInfo uriInfo;

    @Inject
    @Context
    private HttpHeaders headers;

    @Override
    public GeneratedCacheKey
            generateCacheKey(final CacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext) {
        return new GeneratedCacheKeyImpl(Stream
                .concat(Stream.of(cacheKeyInvocationContext.getKeyParameters()).map(CacheInvocationParameter::getValue),
                        getContextualKeys())
                .toArray(Object[]::new));
    }

    private Stream<Object> getContextualKeys() {
        try {
            return Stream
                    .of(uriInfo.getPath(), uriInfo.getQueryParameters(), headers.getLanguage(),
                            headers.getHeaderString(HttpHeaders.ACCEPT),
                            headers.getHeaderString(HttpHeaders.ACCEPT_ENCODING));
        } catch (Exception e) {
            log.debug("[getContextualKeys] context not applicable: {}", e.getMessage());
            return Stream.empty();
        }
    }

    private static class GeneratedCacheKeyImpl implements GeneratedCacheKey {

        private final Object[] params;

        private final int hash;

        private GeneratedCacheKeyImpl(final Object[] parameters) {
            params = parameters;
            hash = Arrays.deepHashCode(parameters);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final GeneratedCacheKeyImpl that = GeneratedCacheKeyImpl.class.cast(o);
            return Arrays.deepEquals(params, that.params);

        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
