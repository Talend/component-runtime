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
package org.talend.sdk.component.junit.lang;

import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import lombok.Data;

@Data
public class StreamDecorator implements InvocationHandler {

    private final BaseStream delegate;

    private final Consumer<Runnable> leafDecorator;

    // if method is iterator() or splitIterator() the behavior is likely not the hoped exact one but ok for us
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        try {
            final boolean stream = BaseStream.class.isAssignableFrom(method.getReturnType());
            final Object result = stream ? method.invoke(delegate, args) : wrap(() -> {
                try {
                    return method.invoke(delegate, args);
                } catch (final IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw toRuntimeException(e);
                }
            });
            if (stream) {
                if (Stream.class.isInstance(result)) {
                    return decorate(Stream.class.cast(result), Stream.class, leafDecorator);
                }
                if (IntStream.class.isInstance(result)) {
                    return decorate(IntStream.class.cast(result), IntStream.class, leafDecorator);
                }
                if (LongStream.class.isInstance(result)) {
                    return decorate(LongStream.class.cast(result), LongStream.class, leafDecorator);
                }
                if (DoubleStream.class.isInstance(result)) {
                    return decorate(DoubleStream.class.cast(result), DoubleStream.class, leafDecorator);
                }
            }
            return result;
        } catch (final InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

    private <V> V wrap(final Supplier<V> supplier) {
        final AtomicReference<V> ref = new AtomicReference<>();
        leafDecorator.accept(() -> ref.set(supplier.get()));
        return ref.get();
    }

    public static <T> Stream<T> decorate(final Stream<T> delegate, final Consumer<Runnable> wrapper) {
        return decorate(delegate, Stream.class, wrapper);
    }

    private static <T, S extends BaseStream<T, ?>> S decorate(final S delegate, final Class<S> type,
            final Consumer<Runnable> wrapper) {
        return (S) Proxy
                .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { type },
                        new StreamDecorator(delegate, wrapper));
    }
}
