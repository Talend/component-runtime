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
package org.talend.sdk.component.server.service.jcache.cdi;

/*
 * NOTE (Talend): This file is adapted from
 * org.apache.geronimo:geronimo-jcache-simple:1.0.5 (Apache License, Version 2.0),
 * class org.apache.geronimo.jcache.simple.cdi.MakeJCacheCDIInterceptorFriendly.
 * It has been repackaged into the CDI/Interceptors "jakarta.*" namespace (JSR-107/
 * "javax.cache.*" types are intentionally left unchanged, since the JCache specification
 * itself has not migrated to a "jakarta.cache" package) so that the JSR-107 declarative
 * caching annotations (@CacheResult, @CachePut, @CacheRemove, @CacheRemoveAll) keep working
 * once component-server runs on a jakarta CDI container.
 *
 * The upstream class also manually re-registered CDIJCacheHelper and the interceptor classes
 * as synthetic AnnotatedTypes (plus a hand-rolled Bean<CDIJCacheHelper> and a
 * ProcessAnnotatedType-based veto) to make them CDI beans in containers using the CDI-default
 * bean-discovery-mode="annotated" without any bean-defining annotation of their own. That
 * machinery has been dropped here: this project's beans.xml uses bean-discovery-mode="all",
 * CDIJCacheHelper already carries @ApplicationScoped and the interceptors already carry
 * 
 * @Interceptor/@Priority, so all of them are naturally discovered by OpenWebBeans on their own.
 * Keeping the extra synthetic registrations on top of that caused OpenWebBeans to see two
 * competing CDIJCacheHelper beans (AmbiguousResolutionException) once ordering assumptions
 * from the original (pre-jakarta) CDI implementation no longer held. Only the interceptor
 * binding registration below remains necessary: the JSR-107 annotations
 * (javax.cache.annotation.*) carry no CDI @InterceptorBinding meta-annotation of their own, so
 * the container needs to be told explicitly to treat them as interceptor bindings.
 * See the original Apache License, Version 2.0 header below, retained from the upstream source
 * file.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.stream.Stream;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

// TODO: observe annotated type (or maybe sthg else) to cache data and inject this extension (used as metadata cache)
// to get class model and this way allow to add cache annotation on the fly - == avoid java pure reflection to get
// metadata
public class MakeJCacheCDIInterceptorFriendly implements Extension {

    private static final boolean SKIP = Boolean.getBoolean("org.apache.geronimo.jcache.simple.skip-cdi");

    public void discoverInterceptorBindings(final @Observes BeforeBeanDiscovery beforeBeanDiscovery,
            final BeanManager bm) {
        if (SKIP) {
            return;
        }
        // JSR-107's annotations (javax.cache.annotation.*) aren't aware of CDI at all, so they
        // need to be registered as interceptor bindings explicitly; CDIJCacheHelper and the
        // interceptor classes are discovered normally (see class-level note above).
        Stream.of(CachePut.class, CacheRemove.class, CacheRemoveAll.class, CacheResult.class)
                .forEach(it -> beforeBeanDiscovery.addInterceptorBinding(bm.createAnnotatedType(it)));
    }
}
