/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.xbean.registry;

import static java.util.Optional.ofNullable;
import static org.apache.xbean.recipe.RecipeHelper.toClass;
import static org.talend.sdk.component.runtime.manager.util.Lazy.lazy;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.xbean.propertyeditor.AbstractConverter;
import org.apache.xbean.propertyeditor.BigDecimalEditor;
import org.apache.xbean.propertyeditor.BigIntegerEditor;
import org.apache.xbean.propertyeditor.BooleanEditor;
import org.apache.xbean.propertyeditor.CharacterEditor;
import org.apache.xbean.propertyeditor.ClassEditor;
import org.apache.xbean.propertyeditor.Converter;
import org.apache.xbean.propertyeditor.DateEditor;
import org.apache.xbean.propertyeditor.DoubleEditor;
import org.apache.xbean.propertyeditor.EnumConverter;
import org.apache.xbean.propertyeditor.FileEditor;
import org.apache.xbean.propertyeditor.HashMapEditor;
import org.apache.xbean.propertyeditor.HashtableEditor;
import org.apache.xbean.propertyeditor.Inet4AddressEditor;
import org.apache.xbean.propertyeditor.Inet6AddressEditor;
import org.apache.xbean.propertyeditor.InetAddressEditor;
import org.apache.xbean.propertyeditor.ListEditor;
import org.apache.xbean.propertyeditor.MapEditor;
import org.apache.xbean.propertyeditor.ObjectNameEditor;
import org.apache.xbean.propertyeditor.PatternConverter;
import org.apache.xbean.propertyeditor.PropertiesEditor;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.apache.xbean.propertyeditor.SetEditor;
import org.apache.xbean.propertyeditor.SortedMapEditor;
import org.apache.xbean.propertyeditor.SortedSetEditor;
import org.apache.xbean.propertyeditor.StringEditor;
import org.apache.xbean.propertyeditor.URIEditor;
import org.apache.xbean.propertyeditor.URLEditor;
import org.talend.sdk.component.runtime.manager.xbean.converter.LocalDateConverter;
import org.talend.sdk.component.runtime.manager.xbean.converter.LocalDateTimeConverter;
import org.talend.sdk.component.runtime.manager.xbean.converter.LocalTimeConverter;
import org.talend.sdk.component.runtime.manager.xbean.converter.ZonedDateTimeConverter;

public class EnrichedPropertyEditorRegistry extends PropertyEditorRegistry {

    private final ThreadLocal<Map<Type, Optional<Converter>>> converterCache = new ThreadLocal<>();

    public EnrichedPropertyEditorRegistry() {
        final DoubleEditor doubleEditor = new DoubleEditor();
        // the front always sends us doubles so
        // if we don't map double to the native number we get number format exceptions
        final BiFunction<Class<?>, Function<Double, Object>, Converter> numberConverter =
                (type, mapper) -> new AbstractConverter(type) {

                    @Override
                    protected Object toObjectImpl(final String text) {
                        if (text.isEmpty()) {
                            return null;
                        }
                        final Object o = doubleEditor.toObject(text);
                        return mapper.apply(Double.class.cast(o));
                    }
                };

        // built-in (was provided by xbean originally)
        super.register(new BooleanEditor());
        super.register(numberConverter.apply(Byte.class, Double::byteValue));
        super.register(numberConverter.apply(Short.class, Double::shortValue));
        super.register(numberConverter.apply(Integer.class, Double::intValue));
        super.register(numberConverter.apply(Long.class, Double::longValue));
        super.register(numberConverter.apply(Float.class, Double::floatValue));
        super.register(doubleEditor);
        super.register(new BigDecimalEditor());
        super.register(new BigIntegerEditor());
        super.register(new StringEditor());
        super.register(new CharacterEditor());
        super.register(new ClassEditor());
        super.register(new LazyDateEditor());
        super.register(new FileEditor());
        super.register(new HashMapEditor());
        super.register(new HashtableEditor());
        super.register(new Inet4AddressEditor());
        super.register(new Inet6AddressEditor());
        super.register(new InetAddressEditor());
        super.register(new ListEditor());
        super.register(new SetEditor());
        super.register(new MapEditor());
        super.register(new SortedMapEditor());
        super.register(new SortedSetEditor());
        super.register(new ObjectNameEditor());
        super.register(new PropertiesEditor());
        super.register(new URIEditor());
        super.register(new URLEditor());
        super.register(new PatternConverter());

        // customs
        super.register(new LazyZonedDateTimeConverter());
        super.register(new LocalDateTimeConverter());
        super.register(new LocalDateConverter());
        super.register(new LocalTimeConverter());

        // extensions
        ServiceLoader.load(Converter.class).forEach(this::register);
    }

    @Override
    public Converter findConverter(final Type type) {
        final Map<Type, Optional<Converter>> cache = converterCache.get();
        if (cache == null) {
            converterCache.remove();
            return doFindConverter(type);
        }
        return ofNullable(cache.get(type)).flatMap(c -> c).orElseGet(() -> {
            final Converter converter = doFindConverter(type);
            cache.put(type, ofNullable(converter));
            return converter;
        });
    }

    public <T> T withCache(final Map<Type, Optional<Converter>> cache, final Supplier<T> task) {
        converterCache.set(cache);
        try {
            return task.get();
        } finally {
            converterCache.remove();
        }
    }

    private Converter doFindConverter(final Type type) {
        return Stream
                .<Supplier<Converter>> of(() -> findInternalConverter(type), () -> findStructuralConverter(type))
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected Converter findStructuralConverter(final Type type) {
        final Class<?> clazz = toClass(type);
        if (Enum.class.isAssignableFrom(clazz)) {
            return new EnumConverter(clazz) {

                @Override
                protected Object toObjectImpl(final String text) {
                    // override default logic, allows convert an empty string to null enum value
                    return text.isEmpty() ? null : super.toObject(text);
                }
            };
        } else {
            return super.findStructuralConverter(type);
        }
    }

    @Override
    public Converter register(final Converter converter) {
        final Map<Type, Optional<Converter>> cache = converterCache.get();
        if (cache != null) {
            cache.putIfAbsent(converter.getType(), ofNullable(converter));
        } else {
            converterCache.remove();
        }
        return converter; // avoid unexpected caching (would leak)
    }

    // used when default init is slow
    private static class LazyEditor<T extends AbstractConverter> extends AbstractConverter {

        private final Supplier<T> delegate;

        private LazyEditor(final Supplier<T> delegate, final Class<?> type) {
            super(type);
            this.delegate = delegate;
        }

        @Override
        protected String toStringImpl(final Object value) {
            return delegate.get().toString(value);
        }

        @Override
        protected Object toObjectImpl(final String text) {
            return delegate.get().toObject(text);
        }
    }

    private static class LazyDateEditor extends LazyEditor<DateEditor> {

        private LazyDateEditor() {
            super(lazy(DateEditor::new), Date.class);
        }
    }

    private static class LazyZonedDateTimeConverter extends LazyEditor<ZonedDateTimeConverter> {

        private LazyZonedDateTimeConverter() {
            super(lazy(ZonedDateTimeConverter::new), Date.class);
        }
    }
}
