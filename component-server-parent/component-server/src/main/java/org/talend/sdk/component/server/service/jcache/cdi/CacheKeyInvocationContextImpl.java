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
 * class org.apache.geronimo.jcache.simple.cdi.CacheKeyInvocationContextImpl.
 * It has been repackaged into the CDI/Interceptors "jakarta.*" namespace (JSR-107/
 * "javax.cache.*" types are intentionally left unchanged, since the JCache specification
 * itself has not migrated to a "jakarta.cache" package) so that the JSR-107 declarative
 * caching annotations (@CacheResult, @CachePut, @CacheRemove, @CacheRemoveAll) keep working
 * once component-server runs on a jakarta CDI container. See the original Apache License,
 * Version 2.0 header below, retained from the upstream source file.
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

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;

import jakarta.interceptor.InvocationContext;

public class CacheKeyInvocationContextImpl<A extends Annotation> extends CacheInvocationContextImpl<A>
        implements CacheKeyInvocationContext<A> {

    private CacheInvocationParameter[] keyParams = null;

    private CacheInvocationParameter valueParam = null;

    public CacheKeyInvocationContextImpl(final InvocationContext delegate, final A annotation, final String name,
            final CDIJCacheHelper.MethodMeta methodMeta) {
        super(delegate, annotation, name, methodMeta);
    }

    @Override
    public CacheInvocationParameter[] getKeyParameters() {
        if (keyParams == null) {
            keyParams = doGetAllParameters(meta.getKeysIndices());
        }
        return keyParams;
    }

    @Override
    public CacheInvocationParameter getValueParameter() {
        if (valueParam == null) {
            valueParam =
                    meta.getValueIndex() >= 0 ? doGetAllParameters(new Integer[] { meta.getValueIndex() })[0] : null;
        }
        return valueParam;
    }
}
