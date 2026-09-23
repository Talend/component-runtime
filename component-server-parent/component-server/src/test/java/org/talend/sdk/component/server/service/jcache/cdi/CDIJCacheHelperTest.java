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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

import org.apache.geronimo.jcache.simple.cdi.CacheResolverFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CDIJCacheHelperTest {

    @Mock
    private BeanManager beanManager;

    private CDIJCacheHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        helper = new CDIJCacheHelper();
        setField(helper, "beanManager", beanManager);
    }

    @Test
    void findMetaCachesMethodMetaPerTargetTypeAndMethod() throws Exception {
        final Target target = new Target();
        final Method method = Target.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta first = helper.findMeta(ic);
        final CDIJCacheHelper.MethodMeta second = helper.findMeta(ic);

        assertSame(first, second, "MethodMeta must be memoized for the same target type + method");
    }

    @Test
    void findMetaResolvesCacheResultAnnotationAndKeyIndices() throws Exception {
        final Target target = new Target();
        final Method method = Target.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertEquals("resultCache", meta.getCacheResultCacheName());
        assertNotNull(meta.getCacheResult());
        assertArrayEquals(new Integer[] { 0 }, meta.getKeysIndices());
        assertEquals(-1, meta.getValueIndex());
        assertFalse(meta.isCompletionStage());
    }

    @Test
    void findMetaDetectsCompletionStageReturnType() throws Exception {
        final Target target = new Target();
        final Method method = Target.class.getDeclaredMethod("loadAsync", String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertTrue(meta.isCompletionStage());
    }

    @Test
    void findMetaFixesValueParameterIndexForCachePutWithLeadingKeyParameter() throws Exception {
        final Target target = new Target();
        final Method method = Target.class.getDeclaredMethod("put", String.class, String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        // regression test for QTDI-3358 round 2's "idx off-by-one" fix: the @CacheValue parameter is the
        // *second* parameter (index 1), not the first.
        assertEquals(1, meta.getValueIndex());
        assertArrayEquals(new Integer[] { 0 }, meta.getKeysIndices());
        assertTrue(meta.isCachePutAfter());
    }

    @Test
    void findMetaUsesAllNonValueParametersAsKeysWhenNoneAreExplicitlyAnnotated() throws Exception {
        final Target target = new Target();
        final Method method = Target.class.getDeclaredMethod("putImplicitKeys", String.class, String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertArrayEquals(new Integer[] { 0 }, meta.getKeysIndices());
        assertEquals(1, meta.getValueIndex());
    }

    @Test
    void findMetaFallsBackToClassLevelCacheDefaultsNameWhenAnnotationNameIsEmpty() throws Exception {
        final DefaultedTarget target = new DefaultedTarget();
        final Method method = DefaultedTarget.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertEquals("fallback-name", meta.getCacheResultCacheName());
    }

    @Test
    void findMetaGeneratesNameFromMethodSignatureWhenNoNameIsAvailable() throws Exception {
        final Target target = new Target();
        final Method method = Target.class.getDeclaredMethod("removeAllNoName");
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertEquals(Target.class.getName() + ".removeAllNoName()", meta.getCacheRemoveAllCacheName());
    }

    @Test
    void findMetaResolvesCacheDefaultsThroughProxyInterface() throws Exception {
        final ProxiedTarget proxy = (ProxiedTarget) Proxy
                .newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ProxiedTarget.class },
                        (InvocationHandler) (p, m, args) -> null);
        final Method method = ProxiedTarget.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockInvocationContext(method, proxy);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertEquals("proxy-fallback", meta.getCacheResultCacheName());
    }

    @Test
    void isIncludedReturnsFalseWhenNoInOrOutClassesAreConfigured() {
        assertFalse(helper.isIncluded(IllegalStateException.class, new Class<?>[0], new Class<?>[0]));
    }

    @Test
    void isIncludedReturnsTrueWhenClassMatchesInAndNotOut() {
        assertTrue(helper
                .isIncluded(IllegalStateException.class, new Class<?>[] { RuntimeException.class },
                        new Class<?>[0]));
    }

    @Test
    void isIncludedReturnsFalseWhenClassMatchesBothInAndOut() {
        assertFalse(helper
                .isIncluded(IllegalStateException.class, new Class<?>[] { RuntimeException.class },
                        new Class<?>[] { IllegalStateException.class }));
    }

    @Test
    void isIncludedReturnsFalseWhenClassDoesNotMatchIn() {
        assertFalse(helper
                .isIncluded(IllegalArgumentException.class, new Class<?>[] { NullPointerException.class },
                        new Class<?>[0]));
    }

    @Test
    void findMetaResolvesCustomCdiKeyGeneratorFromNormalScopedBean() throws Exception {
        final CustomKeyGenerator generatorInstance = new CustomKeyGenerator();
        mockBean(CustomKeyGenerator.class, generatorInstance, true);

        final CustomGeneratorTarget target = new CustomGeneratorTarget();
        final Method method = CustomGeneratorTarget.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertSame(generatorInstance, meta.getCacheResultKeyGenerator());
    }

    @Test
    void findMetaResolvesCustomCdiResolverFactoryFromPseudoScopedBeanAndDefersRelease() throws Exception {
        final CustomResolverFactory factoryInstance = new CustomResolverFactory();
        final CreationalContext<?> context = mockBean(CustomResolverFactory.class, factoryInstance, false);

        final CustomResolverTarget target = new CustomResolverTarget();
        final Method method = CustomResolverTarget.class.getDeclaredMethod("load", String.class);
        final InvocationContext ic = mockInvocationContext(method, target);

        final CDIJCacheHelper.MethodMeta meta = helper.findMeta(ic);

        assertSame(factoryInstance, meta.getCacheResultResolverFactory());
        // pseudo-scoped beans are not released immediately - they are deferred to CDIJCacheHelper#release
        verify(context, never()).release();

        invokePrivate(helper, "release");
        verify(context).release();
    }

    @Test
    void releaseSwallowsRuntimeExceptionRaisedByCreationalContextRelease() throws Exception {
        final CreationalContext<?> failingContext = mock(CreationalContext.class);
        org.mockito.Mockito
                .doThrow(new IllegalStateException("boom"))
                .when(failingContext)
                .release();
        addToRelease(helper, failingContext);

        // must not propagate - CDIJCacheHelper#release logs and continues releasing the remaining contexts
        invokePrivate(helper, "release");
        verify(failingContext).release();
    }

    @Test
    void releaseClosesTheLazilyCreatedDefaultCacheResolverFactory() throws Exception {
        final CacheResolverFactoryImpl mockFactory = mock(CacheResolverFactoryImpl.class);
        setField(helper, "defaultCacheResolverFactory", mockFactory);

        invokePrivate(helper, "release");

        verify(mockFactory).release();
    }

    private InvocationContext mockInvocationContext(final Method method, final Object target) {
        final InvocationContext ic = mock(InvocationContext.class);
        when(ic.getMethod()).thenReturn(method);
        when(ic.getTarget()).thenReturn(target);
        return ic;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> CreationalContext<?> mockBean(final Class<T> type, final T instance, final boolean normalScope) {
        final Bean bean = mock(Bean.class);
        final Set<Bean<?>> beans = new HashSet<>();
        beans.add(bean);
        final CreationalContext context = mock(CreationalContext.class);
        when(beanManager.getBeans(type)).thenReturn(beans);
        when(beanManager.resolve(beans)).thenReturn(bean);
        when(beanManager.createCreationalContext(bean)).thenReturn(context);
        when(bean.getScope()).thenReturn(normalScope ? ApplicationScoped.class : RequestScoped.class);
        when(beanManager.isNormalScope(any())).thenReturn(normalScope);
        when(bean.getBeanClass()).thenReturn((Class) type);
        when(beanManager.getReference(bean, (Class) type, context)).thenReturn(instance);
        return context;
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void addToRelease(final CDIJCacheHelper target, final CreationalContext<?> context)
            throws Exception {
        final Field field = CDIJCacheHelper.class.getDeclaredField("toRelease");
        field.setAccessible(true);
        ((java.util.Collection<CreationalContext<?>>) field.get(target)).add(context);
    }

    private static void invokePrivate(final Object target, final String name) throws Exception {
        final Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        try {
            method.invoke(target);
        } catch (final java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    private static class Target {

        @CacheResult(cacheName = "resultCache")
        String load(@CacheKey final String id) {
            return null;
        }

        @CacheResult(cacheName = "asyncCache")
        CompletionStage<String> loadAsync(@CacheKey final String id) {
            return null;
        }

        @CachePut
        void put(@CacheKey final String id, @CacheValue final String value) {
            // no-op, only annotation metadata matters for these tests
        }

        @CachePut
        void putImplicitKeys(final String id, @CacheValue final String value) {
            // no-op - id has no explicit @CacheKey, it must still be inferred as a key parameter
        }

        @CacheRemoveAll
        void removeAllNoName() {
            // no-op
        }
    }

    @CacheDefaults(cacheName = "fallback-name")
    private static class DefaultedTarget {

        @CacheResult
        String load(@CacheKey final String id) {
            return null;
        }
    }

    @CacheDefaults(cacheName = "proxy-fallback")
    private interface ProxiedTarget {

        @CacheResult
        String load(@CacheKey final String id);
    }

    private static class CustomKeyGenerator implements CacheKeyGenerator {

        @Override
        public javax.cache.annotation.GeneratedCacheKey generateCacheKey(
                final javax.cache.annotation.CacheKeyInvocationContext<? extends java.lang.annotation.Annotation> context) {
            return null;
        }
    }

    private static class CustomGeneratorTarget {

        @CacheResult(cacheName = "customGenCache", cacheKeyGenerator = CustomKeyGenerator.class)
        String load(@CacheKey final String id) {
            return null;
        }
    }

    private static class CustomResolverFactory implements CacheResolverFactory {

        @Override
        public javax.cache.annotation.CacheResolver getCacheResolver(
                final javax.cache.annotation.CacheMethodDetails<? extends java.lang.annotation.Annotation> details) {
            return null;
        }

        @Override
        public javax.cache.annotation.CacheResolver getExceptionCacheResolver(
                final javax.cache.annotation.CacheMethodDetails<CacheResult> details) {
            return null;
        }
    }

    private static class CustomResolverTarget {

        @CacheResult(cacheName = "customResolverCache", cacheResolverFactory = CustomResolverFactory.class)
        String load(@CacheKey final String id) {
            return null;
        }
    }
}
