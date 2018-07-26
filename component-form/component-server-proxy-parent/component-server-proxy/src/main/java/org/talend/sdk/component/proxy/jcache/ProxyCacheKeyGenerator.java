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
package org.talend.sdk.component.proxy.jcache;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;

@ApplicationScoped
public class ProxyCacheKeyGenerator implements CacheKeyGenerator {

    @Inject
    private ProxyConfiguration configuration;

    @Override
    public GeneratedCacheKey generateCacheKey(final CacheKeyInvocationContext<? extends Annotation> ctx) {
        return new GeneratedCacheKeyImpl(Stream.of(ctx.getKeyParameters()).map(this::toValue).toArray());
    }

    private Object toValue(final CacheInvocationParameter it) {
        final Object value = it.getValue();

        // placeholder provider, replace it by actual values to keep the cache consistent (lang etc)
        if (it.getRawType() == Function.class) {
            if (configuration.getCacheHeaderName().isPresent()) {
                return Function.class.cast(it.getValue()).apply(configuration.getCacheHeaderName().get());
            }
            final Collection<String> headers = configuration.getDynamicHeaders();
            if (headers.isEmpty()) {
                return null;
            }
            final Function<String, String> fn = Function.class.cast(value);
            return headers.stream().map(fn).toArray();
        }

        return value;
    }

    protected class GeneratedCacheKeyImpl implements GeneratedCacheKey {

        private final Object[] params;

        private final int hash;

        private GeneratedCacheKeyImpl(final Object[] parameters) {
            this.params = parameters;
            this.hash = Arrays.deepHashCode(parameters);
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
            if (that.hash != hash) { // quick exit path
                return false;
            }
            return Arrays.deepEquals(params, that.params);

        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
