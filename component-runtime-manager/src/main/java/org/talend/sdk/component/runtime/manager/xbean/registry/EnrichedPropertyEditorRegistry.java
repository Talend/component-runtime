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
        register(new BooleanEditor());
        register(numberConverter.apply(Byte.class, Double::byteValue));
        register(numberConverter.apply(Short.class, Double::shortValue));
        register(numberConverter.apply(Integer.class, Double::intValue));
        register(numberConverter.apply(Long.class, Double::longValue));
        register(numberConverter.apply(Float.class, Double::floatValue));
        register(doubleEditor);
        register(new BigDecimalEditor());
        register(new BigIntegerEditor());
        register(new StringEditor());
        register(new CharacterEditor());
        register(new ClassEditor());
        register(new DateEditor());
        register(new FileEditor());
        register(new HashMapEditor());
        register(new HashtableEditor());
        register(new Inet4AddressEditor());
        register(new Inet6AddressEditor());
        register(new InetAddressEditor());
        register(new ListEditor());
        register(new SetEditor());
        register(new MapEditor());
        register(new SortedMapEditor());
        register(new SortedSetEditor());
        register(new ObjectNameEditor());
        register(new PropertiesEditor());
        register(new URIEditor());
        register(new URLEditor());
        register(new PatternConverter());

        // customs
        register(new ZonedDateTimeConverter());
        register(new LocalDateTimeConverter());
        register(new LocalDateConverter());
        register(new LocalTimeConverter());

        // extensions
        ServiceLoader.load(Converter.class).forEach(this::register);
    }
}
