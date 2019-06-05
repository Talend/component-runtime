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
package org.talend.sdk.component.runtime.manager.xbean.registry;

import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.xbean.propertyeditor.AbstractConverter;
import org.apache.xbean.propertyeditor.BigDecimalEditor;
import org.apache.xbean.propertyeditor.BigIntegerEditor;
import org.apache.xbean.propertyeditor.BooleanEditor;
import org.apache.xbean.propertyeditor.CharacterEditor;
import org.apache.xbean.propertyeditor.ClassEditor;
import org.apache.xbean.propertyeditor.Converter;
import org.apache.xbean.propertyeditor.DateEditor;
import org.apache.xbean.propertyeditor.DoubleEditor;
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

    public EnrichedPropertyEditorRegistry() {
        final PropertyEditorRegistry registry = new PropertyEditorRegistry();
        final DoubleEditor doubleEditor = new DoubleEditor();
        // the front always sends us doubles so
        // if we don't map double to the native number we get number format exceptions
        final BiFunction<Class<?>, Function<Double, Object>, Converter> numberConverter =
                (type, mapper) -> new AbstractConverter(type) {

                    @Override
                    protected Object toObjectImpl(final String text) {
                        final Object o = doubleEditor.toObject(text);
                        return mapper.apply(Double.class.cast(o));
                    }
                };

        // built-in (was provided by xbean originally)
        registry.register(new BooleanEditor());
        registry.register(numberConverter.apply(Byte.class, Double::byteValue));
        registry.register(numberConverter.apply(Short.class, Double::shortValue));
        registry.register(numberConverter.apply(Integer.class, Double::intValue));
        registry.register(numberConverter.apply(Long.class, Double::longValue));
        registry.register(numberConverter.apply(Float.class, Double::floatValue));
        registry.register(doubleEditor);
        registry.register(new BigDecimalEditor());
        registry.register(new BigIntegerEditor());
        registry.register(new StringEditor());
        registry.register(new CharacterEditor());
        registry.register(new ClassEditor());
        registry.register(new DateEditor());
        registry.register(new FileEditor());
        registry.register(new HashMapEditor());
        registry.register(new HashtableEditor());
        registry.register(new Inet4AddressEditor());
        registry.register(new Inet6AddressEditor());
        registry.register(new InetAddressEditor());
        registry.register(new ListEditor());
        registry.register(new SetEditor());
        registry.register(new MapEditor());
        registry.register(new SortedMapEditor());
        registry.register(new SortedSetEditor());
        registry.register(new ObjectNameEditor());
        registry.register(new PropertiesEditor());
        registry.register(new URIEditor());
        registry.register(new URLEditor());
        registry.register(new PatternConverter());

        // customs
        registry.register(new ZonedDateTimeConverter());
        registry.register(new LocalDateTimeConverter());
        registry.register(new LocalDateConverter());
        registry.register(new LocalTimeConverter());

        // extensions
        ServiceLoader.load(Converter.class).forEach(registry::register);
    }
}
