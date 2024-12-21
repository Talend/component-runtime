/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;

import org.apache.xbean.propertyeditor.AbstractConverter;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.Pattern;
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.widget.DateTime;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.runtime.internationalization.InternationalizationServiceFactory;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService.Messages;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalCacheService;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.service.configuration.PropertiesConfiguration;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.manager.test.MethodsHolder;
import org.talend.sdk.component.runtime.manager.util.MemoizingSupplier;
import org.talend.sdk.component.runtime.manager.xbean.converter.ZonedDateTimeConverter;

import lombok.Data;
import lombok.EqualsAndHashCode;

class ReflectionServiceTest {

    private static final Messages MESSAGES = new InternationalizationServiceFactory(Locale::getDefault)
            .create(Messages.class, ReflectionServiceTest.class.getClassLoader());

    private final PropertyEditorRegistry registry = new PropertyEditorRegistry() {

        {
            register(new AbstractConverter(Integer.class) {

                @Override
                protected Object toObjectImpl(final String text) {
                    return Double.valueOf(text).intValue();
                }
            });
            register(new ZonedDateTimeConverter());
        }
    };

    private final ParameterModelService parameterModelService = new ParameterModelService(registry);

    private final ReflectionService reflectionService = new ReflectionService(parameterModelService, registry);

    @Test
    void jsonObject() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(JsonObject.class, new HashMap<>());
        final Object[] objects = factory.apply(singletonMap("root", "{\"set\":true}"));
        assertEquals(Json.createObjectBuilder().add("set", true).build(), JsonObject.class.cast(objects[0]));
    }

    @Test
    void date() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory =
                getComponentFactory(ConfigWithDate.class, new HashMap<>());
        final ZonedDateTime expected = ZonedDateTime.now();
        final Object[] objects = factory.apply(singletonMap("root.date", expected.toString()));
        assertEquals(expected, ConfigWithDate.class.cast(objects[0]).date);
    }

    @Test
    void renamedOption() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory =
                getComponentFactory(RenamedOption.class, new HashMap<>());
        final Object[] objects = factory.apply(singletonMap("configuration.$url", "set"));
        assertEquals("set", objects[0].toString());
    }

    @Test
    void nestedRenamedOption() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory =
                getComponentFactory(NestedRenamedOption.class, new HashMap<>());
        final Object[] objects = factory.apply(singletonMap("configuration.$option.$url", "set"));
        assertEquals("set", objects[0].toString());
    }

    @Test
    void configurationFromLocalConf() throws NoSuchMethodException {
        final Properties properties = new Properties();
        properties.setProperty("test.myconfig.url", "http://foo");
        properties.setProperty("myconfig.user", "set");
        final Supplier<ScheduledExecutorService> executor = new MemoizingSupplier<>(this::buildExecutorService);
        final Function<Map<String, String>, Object[]> factory =
                getComponentFactory(MyConfig.class, new HashMap<Class<?>, Object>() {

                    {
                        put(LocalConfiguration.class, //
                                new LocalConfigurationService(singletonList(new PropertiesConfiguration(properties)),
                                        "test"));
                        put(LocalCache.class, //
                                new LocalCacheService("test", System::currentTimeMillis, executor));
                    }
                });
        final MyConfig myConfig = MyConfig.class.cast(factory.apply(new HashMap<String, String>() {

            {
                put("myconfig.url", "ignored");
            }
        })[0]);
        assertEquals("http://foo", myConfig.url);
        assertEquals("set", myConfig.user);
    }

    private ScheduledExecutorService buildExecutorService() {
        return Executors.newScheduledThreadPool(4, (Runnable r) -> {
            final Thread thread = new Thread(r, "ReflectionTest-" + hashCode());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
    }

    @Test
    void validationRequiredStringOk() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig.class);
        {
            SomeConfig.class.cast(factory.apply(new HashMap<String, String>() {

                {
                    put("root.requiredString", "set");
                    put("root.integer", "5");
                }
            })[0]).isSet("set", 5);
        }
        {
            SomeConfig.class.cast(factory.apply(new HashMap<String, String>() {

                {
                    put("root.requiredString", "set2");
                    put("root.integer", "10");
                }
            })[0]).isSet("set2", 10);
        }
    }

    @Test
    void instantiateEvenWithBadNumberType() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig.class);
        SomeConfig.class.cast(factory.apply(new HashMap<String, String>() {

            {
                put("root.requiredString", "set");
                put("root.integer", "5.0");
            }
        })[0]).isSet("set", 5);
    }

    @Test
    void validationRequiredNotVisiblePrimitive() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredVisibilityPrimitive.class);
        assertNull(RequiredVisibilityPrimitive.class.cast(factory.apply(new HashMap<String, String>() {

            {
                put("root.toggle", "false");
            }
        })[0]).string);
    }

    @Test
    void validationRequiredVisiblePrimitiveInvalid() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredVisibilityPrimitive.class);
        assertEquals("- Property 'root.string' is required.",
                assertThrows(IllegalArgumentException.class, () -> factory.apply(new HashMap<String, String>() {

                    {
                        put("root.toggle", "true");
                    }
                })).getMessage());
    }

    @Test
    void validationRequiredVisiblePrimitiveValid() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredVisibilityPrimitive.class);
        assertEquals("sthg", RequiredVisibilityPrimitive.class.cast(factory.apply(new HashMap<String, String>() {

            {
                put("root.toggle", "true");
                put("root.string", "sthg");
            }
        })[0]).string);
    }

    @Test
    void validationRequiredNotVisibleArray() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredVisibilityArray.class);
        assertNull(RequiredVisibilityArray.class.cast(factory.apply(new HashMap<String, String>() {

            {
                put("root.toggle", "false");
            }
        })[0]).strings);
    }

    @Test
    void validationRequiredVisibleArrayInvalid() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredVisibilityArray.class);
        assertThrows(IllegalArgumentException.class, () -> factory.apply(new HashMap<String, String>() {

            {
                put("root.toggle", "true");
            }
        }));
    }

    @Test
    void validationRequiredVisibleArrayValid() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredVisibilityArray.class);
        assertEquals(singletonList("sthg"),
                RequiredVisibilityArray.class.cast(factory.apply(new HashMap<String, String>() {

                    {
                        put("root.toggle", "true");
                        put("root.strings[0]", "sthg");
                    }
                })[0]).strings);
    }

    @Test
    void validationRequiredStringKo() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig.class);
        assertThrows(IllegalArgumentException.class, () -> factory.apply(emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> factory.apply(singletonMap("root.integer", "5")));
        assertThrows(IllegalArgumentException.class, () -> factory.apply(new HashMap<String, String>() {

            {
                put("root.requiredString", "set");
                put("root.integer", "4");
            }
        }));
    }

    @Test
    void validationRequiredList() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredList.class);
        factory.apply(new HashMap<String, String>() {

            {
                put("root.list[0]", "1");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> factory.apply(emptyMap()));
    }

    @Test
    void validationRequiredListObject() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(RequiredListObject.class);
        factory.apply(new HashMap<String, String>() {

            {
                put("root.list[0].regex", "az");
            }
        });
        assertThrows(IllegalArgumentException.class, () -> factory.apply(emptyMap()));
    }

    @Test
    void validationMinListKo() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig2.class);
        assertThrows(IllegalArgumentException.class, () -> factory.apply(new HashMap<String, String>() {

            {
                put("root.integers[0]", "1");
                put("root.integers[1]", "2");
            }
        }));
    }

    @Test
    void validationMinListOk() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig2.class);
        assertEquals(1,
                SomeConfig2.class.cast(factory.apply(singletonMap("root.integers[0]", "1"))[0]).integers.size());
    }

    @Test
    void validationNestedObjectOk() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig3.class);
        assertEquals("somevalue",
                SomeConfig3.class.cast(factory.apply(singletonMap("root.nested.value", "somevalue"))[0]).nested.value);
    }

    @Test
    void validationNestedObjectKo() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig3.class);
        assertThrows(IllegalArgumentException.class, () -> factory.apply(singletonMap("root.nested.value", "short")));
    }

    @Test
    void validationRegexOk() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig5.class);
        assertEquals("somevalue",
                SomeConfig5.class.cast(factory.apply(singletonMap("root.regex", "somevalue"))[0]).regex);
    }

    @Test
    void validationRegexKo() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig5.class);
        assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.regex", "short and another word")));
    }

    @Test
    void validationUrlRegexOk() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig6.class);
        assertEquals("pulsar://localhost:12345",
                SomeConfig6.class
                        .cast(factory.apply(singletonMap("root.pulsar", "pulsar://localhost:12345"))[0]).pulsar);
        assertEquals("pulsar+ssl://localhost:12345",
                SomeConfig6.class
                        .cast(factory.apply(singletonMap("root.pulsar", "pulsar+ssl://localhost:12345"))[0]).pulsar);
        assertEquals("http://localhost:12345",
                SomeConfig6.class.cast(factory.apply(singletonMap("root.url", "http://localhost:12345"))[0]).url);
        assertEquals("https://localhost:12345",
                SomeConfig6.class.cast(factory.apply(singletonMap("root.url", "https://localhost:12345"))[0]).url);
    }

    @Test
    void validationUrlRegexKo() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig6.class);
        assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.pulsar", "pulsar:localhost:12345")));
        assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.pulsar", "pulsar+ssl:localhost:12345")));
        assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.url", "https://localhost:12345 ")));
        assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.url", "mailto://me@talend.com")));
    }

    @Test
    void validationNestedListOk() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig4.class);
        assertEquals("somevalue",
                SomeConfig4.class.cast(factory.apply(singletonMap("root.nesteds[0].value", "somevalue"))[0]).nesteds
                        .iterator()
                        .next().value);
    }

    @Test
    void validationNestedListKo() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeConfig4.class);
        assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.nesteds[0].value", "short")));
    }

    @Test
    void validationIntegerConstraints() throws NoSuchMethodException {
        final Function<Map<String, String>, Object[]> factory = getComponentFactory(SomeIntegerConfig.class);
        // check implicit integer constraint (null value is acceptable value when we have constraint)
        final Object[] result1 = factory.apply(emptyMap());
        assertEquals(1, result1.length);
        assertEquals(new SomeIntegerConfig(), result1[0]);

        // primitive
        // check implicit integer constraint (shouldn't exceed max value by default)
        final IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.primitiveField", String.valueOf(Long.MAX_VALUE))));
        assertEquals("- " + MESSAGES.max("root.primitiveField", Integer.MAX_VALUE, Long.MAX_VALUE),
                exception1.getMessage());

        // check implicit integer constraint (shouldn't exceed min value by default)
        final IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.primitiveField", String.valueOf(Long.MIN_VALUE))));
        assertEquals("- " + MESSAGES.min("root.primitiveField", Integer.MIN_VALUE, Long.MIN_VALUE),
                exception2.getMessage());

        // object
        // check implicit integer constraint (shouldn't exceed max value by default)
        final IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.objectField", String.valueOf(Long.MAX_VALUE))));
        assertEquals("- " + MESSAGES.max("root.objectField", Integer.MAX_VALUE, Long.MAX_VALUE),
                exception3.getMessage());

        // check implicit integer constraint (shouldn't exceed min value by default)
        final IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
                () -> factory.apply(singletonMap("root.objectField", String.valueOf(Long.MIN_VALUE))));
        assertEquals("- " + MESSAGES.min("root.objectField", Integer.MIN_VALUE, Long.MIN_VALUE),
                exception4.getMessage());
    }

    @Test
    void copiable() throws NoSuchMethodException {
        final Map<Class<?>, Object> precomputed = new HashMap<>();
        precomputed
                .put(UserHttpClient.class,
                        new HttpClientFactoryImpl("test", reflectionService, JsonbBuilder.create(), emptyMap())
                                .create(UserHttpClient.class, "http://foo"));
        final Method httpMtd = TableOwner.class.getMethod("http", UserHttpClient.class);
        final HttpClient client1 = HttpClient.class
                .cast(reflectionService.parameterFactory(httpMtd, precomputed, null).apply(emptyMap())[0]);
        final HttpClient client2 = HttpClient.class
                .cast(reflectionService.parameterFactory(httpMtd, precomputed, null).apply(emptyMap())[0]);
        assertNotSame(client1, client2);
        final InvocationHandler handler1 = Proxy.getInvocationHandler(client1);
        final InvocationHandler handler2 = Proxy.getInvocationHandler(client2);
        assertNotSame(handler1, handler2);
        assertEquals(handler1.toString(), handler2.toString());
    }

    @Test
    void primitive() throws NoSuchMethodException {
        { // from string
            final Object[] params = reflectionService
                    .parameterFactory(
                            MethodsHolder.class.getMethod("primitives", String.class, String.class, int.class),
                            emptyMap(), null)
                    .apply(new HashMap<String, String>() {

                        {
                            put("url", "http://foo");
                            put("defaultName", "default");
                            put("port", "1");
                        }
                    });
            assertEquals("http://foo", params[0]);
            assertEquals("default", params[1]);
            assertEquals(1, params[2]);
        }
        { // partial
            final Object[] params = reflectionService
                    .parameterFactory(
                            MethodsHolder.class.getMethod("primitives", String.class, String.class, int.class),
                            emptyMap(), null)
                    .apply(new HashMap<String, String>() {

                        {
                            put("port", "1");
                        }
                    });
            assertNull(params[0]);
            assertNull(params[1]);
            assertEquals(1, params[2]);
        }
        { // exact type
            final Object[] params = reflectionService
                    .parameterFactory(
                            MethodsHolder.class.getMethod("primitives", String.class, String.class, int.class),
                            emptyMap(), null)
                    .apply(new HashMap<String, String>() {

                        {
                            put("port", "1");
                        }
                    });
            assertEquals(1, params[2]);
        }
    }

    @Test
    void collection() throws NoSuchMethodException {
        final Object[] params = reflectionService
                .parameterFactory(MethodsHolder.class.getMethod("collections", List.class, List.class, Map.class),
                        emptyMap(), null)
                .apply(new HashMap<String, String>() {

                    {
                        put("urls[0]", "http://foo");
                        put("urls[1]", "https://bar");
                        put("ports[0]", "1234");
                        put("ports[1]", "5678");
                        put("mapping.key[0]", "key1");
                        put("mapping.value[0]", "value1");
                        put("mapping.key[1]", "key2");
                        put("mapping.value[1]", "value2");
                    }
                });
        assertEquals(asList("http://foo", "https://bar"), params[0]);
        assertEquals(asList(1234, 5678), params[1]);
        assertEquals(new HashMap<String, String>() {

            {
                put("key1", "value1");
                put("key2", "value2");
            }
        }, params[2]);
    }

    @Test
    void array() throws NoSuchMethodException {
        final Object[] params = reflectionService
                .parameterFactory(MethodsHolder.class.getMethod("array", MethodsHolder.Array.class), emptyMap(), null)
                .apply(new HashMap<String, String>() {

                    {
                        put("value.urls[0]", "http://foo");
                        put("value.urls[1]", "https://bar");
                    }
                });
        assertEquals(1, params.length);
        assertTrue(MethodsHolder.Array.class.isInstance(params[0]));
        assertArrayEquals(new String[] { "http://foo", "https://bar" },
                MethodsHolder.Array.class.cast(params[0]).getUrls());
    }

    @Test
    void truncatedArray() throws NoSuchMethodException {
        final Object[] params = reflectionService
                .parameterFactory(MethodsHolder.class.getMethod("array", MethodsHolder.Array.class), emptyMap(), null)
                .apply(new HashMap<String, String>() {

                    {
                        put("value.urls[0]", "http://foo");
                        put("value.urls[1]", "https://bar");
                        put("value.urls[length]", "1");
                    }
                });
        assertEquals(1, params.length);
        assertTrue(MethodsHolder.Array.class.isInstance(params[0]));
        assertArrayEquals(new String[] { "http://foo" }, MethodsHolder.Array.class.cast(params[0]).getUrls());
    }

    @Test
    void object() throws NoSuchMethodException {
        final Object[] params =
                reflectionService
                        .parameterFactory(
                                MethodsHolder.class
                                        .getMethod("object", MethodsHolder.Config.class, MethodsHolder.Config.class),
                                emptyMap(), null)
                        .apply(new HashMap<String, String>() {

                            {
                                put("implicit.urls[0]", "http://foo");
                                put("implicit.urls[1]", "https://bar");
                                put("prefixed.urls[0]", "http://foo2");
                                put("prefixed.urls[1]", "https://bar2");

                                put("implicit.mapping.key[0]", "key1");
                                put("implicit.mapping.value[0]", "val1");
                                put("implicit.mapping.key[1]", "key2");
                                put("implicit.mapping.value[1]", "val2");
                            }
                        });
        Stream.of(params).forEach(p -> assertTrue(MethodsHolder.Config.class.isInstance(p)));
        final MethodsHolder.Config[] configs =
                Stream.of(params).map(MethodsHolder.Config.class::cast).toArray(MethodsHolder.Config[]::new);
        assertEquals(asList("http://foo", "https://bar"), configs[0].getUrls());
        assertEquals(asList("http://foo2", "https://bar2"), configs[1].getUrls());
        assertEquals(new HashMap<String, String>() {

            {
                put("key1", "val1");
                put("key2", "val2");
            }
        }, configs[0].getMapping());
        assertNull(configs[1].getMapping());
    }

    @Test
    void nestedObject() throws NoSuchMethodException {
        final Object[] params = reflectionService
                .parameterFactory(MethodsHolder.class.getMethod("nested", MethodsHolder.ConfigOfConfig.class),
                        emptyMap(), emptyList())
                .apply(new HashMap<String, String>() {

                    {
                        put("value.direct.urls[0]", "http://foo");
                        put("value.direct.urls[1]", "https://bar");
                        put("value.multiple[0].urls[0]", "http://foo1");
                        put("value.multiple[0].urls[1]", "https://bar1");
                        put("value.multiple[1].urls[0]", "http://foo2");
                        put("value.multiple[1].urls[1]", "https://bar2");
                        put("value.keyed.key[0]", "k1");
                        put("value.keyed.value[0].urls[0]", "v1");
                        put("value.keyed.value[0].urls[1]", "v2");
                        put("value.keyed.key[1]", "k2");
                        put("value.keyed.value[1].urls[0]", "v3");
                        put("value.keyed.value[1].urls[1]", "v4");
                        put("value.passthrough", "ok");
                    }
                });
        assertTrue(MethodsHolder.ConfigOfConfig.class.isInstance(params[0]));
        final MethodsHolder.ConfigOfConfig value = MethodsHolder.ConfigOfConfig.class.cast(params[0]);
        assertEquals("ok", value.getPassthrough());
        assertNotNull(value.getDirect());
        assertEquals(asList("http://foo", "https://bar"), value.getDirect().getUrls());
        assertNotNull(value.getMultiple());
        assertEquals(2, value.getMultiple().size());
        assertEquals(asList("http://foo1", "https://bar1"), value.getMultiple().get(0).getUrls());
        assertEquals(asList("http://foo2", "https://bar2"), value.getMultiple().get(1).getUrls());
        assertEquals(2, value.getKeyed().size());
        assertEquals(new HashSet<>(asList("k1", "k2")), value.getKeyed().keySet());
        assertEquals(asList("v1", "v2"), value.getKeyed().get("k1").getUrls());
        assertEquals(asList("v3", "v4"), value.getKeyed().get("k2").getUrls());
    }

    @Test
    void tables() throws NoSuchMethodException {
        final Method factory = TableOwner.class.getMethod("factory", TableOwner.class);
        final PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistry();
        final Object[] tests =
                new ReflectionService(new ParameterModelService(propertyEditorRegistry), propertyEditorRegistry)
                        .parameterFactory(factory, emptyMap(), null)
                        .apply(new HashMap<String, String>() {

                            {
                                put("root.table[0].value1", "test1");
                                put("root.table[0].value2", "12");
                                put("root.table[1].value1", "test2");
                                put("root.table[1].value2", "22");
                                put("root.table[1].nestedList[0].value1", "nested");
                                put("root.table[1].nestedList[0].value2", "1");
                                put("root.map.key[0]", "test1k");
                                put("root.map.value[0].value1", "test1v");
                                put("root.map.key[1]", "test2k");
                                put("root.map.value[1].value1", "test2v");
                            }
                        });
        assertEquals(1, tests.length);
        assertTrue(TableOwner.class.isInstance(tests[0]));

        final TableOwner tableOwner = TableOwner.class.cast(tests[0]);
        {
            assertNotNull(tableOwner.table);
            assertEquals(2, tableOwner.table.size());
            assertEquals(Stream.of("test1", "test2").collect(toList()),
                    tableOwner.table.stream().map(Column::getValue1).collect(toList()));
            assertArrayEquals(IntStream.of(12, 22).toArray(),
                    tableOwner.table.stream().mapToInt(Column::getValue2).toArray());
            assertNotNull(tableOwner.table.get(1).nestedList);
            assertEquals(1, tableOwner.table.get(1).nestedList.size());
            assertEquals("nested", tableOwner.table.get(1).nestedList.get(0).value1);
            assertEquals(1, tableOwner.table.get(1).nestedList.get(0).value2);
        }
        {
            assertNotNull(tableOwner.map);
            assertEquals(2, tableOwner.map.size());
            assertEquals("test1v", tableOwner.map.get("test1k").value1);
            assertEquals("test2v", tableOwner.map.get("test2k").value1);
        }
    }

    @Test
    void truncatedObjectArray() throws NoSuchMethodException {
        final Method factory = TableOwner.class.getMethod("factory", TableOwner.class);
        final PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistry();
        final Object[] tests =
                new ReflectionService(new ParameterModelService(propertyEditorRegistry), propertyEditorRegistry)
                        .parameterFactory(factory, emptyMap(), null)
                        .apply(new HashMap<String, String>() {

                            {
                                put("root.table[0].value1", "test1");
                                put("root.table[0].value2", "12");
                                put("root.table[1].value1", "test2");
                                put("root.table[1].value2", "22");
                                put("root.table[0].nestedList[0].value1", "nested");
                                put("root.table[0].nestedList[0].value2", "1");
                                put("root.table[0].nestedList[length]", "0");
                                put("root.table[length]", "1");
                            }
                        });
        assertEquals(1, tests.length);
        assertTrue(TableOwner.class.isInstance(tests[0]));

        final TableOwner tableOwner = TableOwner.class.cast(tests[0]);
        assertNotNull(tableOwner.table);
        assertEquals(1, tableOwner.table.size());
        assertEquals(singletonList("test1"), tableOwner.table.stream().map(Column::getValue1).collect(toList()));
        assertArrayEquals(new int[] { 12 }, tableOwner.table.stream().mapToInt(Column::getValue2).toArray());
        assertNotNull(tableOwner.table.get(0).nestedList);
        assertTrue(tableOwner.table.get(0).nestedList.isEmpty());
    }

    @Test
    void nestedRequiredActiveIf() throws NoSuchMethodException {
        final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());
        final List<ParameterMeta> metas = service
                .buildParameterMetas(MethodsHolder.class.getMethod("visibility", MethodsHolder.MyDatastore.class),
                        "def", new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        final Object[] params = reflectionService
                .parameterFactory(MethodsHolder.class.getMethod("visibility", MethodsHolder.MyDatastore.class),
                        emptyMap(), metas)
                .apply(new HashMap<String, String>() {

                    {
                        put("value.aString", "foo");
                        put("value.complexConfig", "false");
                    }
                });

        assertTrue(MethodsHolder.MyDatastore.class.isInstance(params[0]));
        final MethodsHolder.MyDatastore value = MethodsHolder.MyDatastore.class.cast(params[0]);
        assertEquals("foo", value.getAString());
        assertFalse(value.isComplexConfig());
    }

    @Test
    void nestedRequiredActiveIfWithTrue() throws NoSuchMethodException {
        final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());
        final List<ParameterMeta> metas = service
                .buildParameterMetas(MethodsHolder.class.getMethod("visibility", MethodsHolder.MyDatastore.class),
                        "def", new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        final Object[] params = reflectionService
                .parameterFactory(MethodsHolder.class.getMethod("visibility", MethodsHolder.MyDatastore.class),
                        emptyMap(), metas)
                .apply(new HashMap<String, String>() {

                    {
                        put("value.aString", "foo");
                        put("value.complexConfig", "true");
                        put("value.complexConfiguration.url", "https://talend.com");
                    }
                });

        assertTrue(MethodsHolder.MyDatastore.class.isInstance(params[0]));
        final MethodsHolder.MyDatastore value = MethodsHolder.MyDatastore.class.cast(params[0]);
        assertEquals("foo", value.getAString());
        assertTrue(value.isComplexConfig());
        assertEquals("https://talend.com", value.getComplexConfiguration().getUrl());
    }

    @Test
    void nestedRequiredActiveIfWithWrongPattern() throws NoSuchMethodException {
        final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());
        final List<ParameterMeta> metas = service
                .buildParameterMetas(MethodsHolder.class.getMethod("visibility", MethodsHolder.MyDatastore.class),
                        "def", new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        assertThrows(IllegalArgumentException.class,
                () -> reflectionService
                        .parameterFactory(MethodsHolder.class.getMethod("visibility", MethodsHolder.MyDatastore.class),
                                emptyMap(), metas)
                        .apply(new HashMap<String, String>() {

                            {
                                put("value.aString", "foo");
                                put("value.complexConfig", "true");
                                put("value.complexConfiguration.url", "");
                            }
                        }));

    }

    @Test
    void nestedRequiredActiveIf_Rest() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("value.apiDesc.loadAPI", "false");
        final Object[] params = buildObjectParams("visibility", payload, MethodsHolder.RestDatastore.class);
        assertTrue(MethodsHolder.RestDatastore.class.isInstance(params[0]));
    }

    @Test
    void nestedRequiredActiveIfTrue_Rest() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("value.apiDesc.loadAPI", "true");
        payload.put("value.complexConfiguration.url", "https://talend.com");
        final Object[] params = buildObjectParams("visibility", payload, MethodsHolder.RestDatastore.class);
        assertTrue(MethodsHolder.RestDatastore.class.isInstance(params[0]));
        final MethodsHolder.RestDatastore value = MethodsHolder.RestDatastore.class.cast(params[0]);
        assertTrue(value.getApiDesc().isLoadAPI());
        assertEquals("https://talend.com", value.getComplexConfiguration().getUrl());
    }

    @Test
    void nestedRequiredActiveIfWrong_Rest() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("value.apiDesc.loadAPI", "true");
        payload.put("value.complexConfiguration.url", "");
        assertThrows(IllegalArgumentException.class,
                () -> buildObjectParams("visibility", payload, MethodsHolder.RestDatastore.class));
    }

    @Test
    void simpleRequiredActiveIfFiltersOk() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.logicalOpType", "ALL");
        payload.put("configuration.logicalOpValue", "ALL");
        final Object[] params = buildObjectParams("visibility", payload, MethodsHolder.FilterConfiguration.class);
        assertTrue(MethodsHolder.FilterConfiguration.class.isInstance(params[0]));
        final MethodsHolder.FilterConfiguration value = MethodsHolder.FilterConfiguration.class.cast(params[0]);
        assertEquals("ALL", value.getLogicalOpType());
        assertEquals("ALL", value.getLogicalOpValue());
    }

    @Test
    void simpleRequiredActiveIfFiltersKo() throws NoSuchMethodException {
        final String expected = "- Property 'configuration.logicalOpValue' is required.";
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.logicalOpType", "ALL");
        try {
            buildObjectParams("visibility", payload, MethodsHolder.FilterConfiguration.class);
        } catch (IllegalArgumentException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    void simpleNestedRequiredActiveIfFiltersOk() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.logicalOpType", "AL");
        payload.put("configuration.filters[0].columnName", "col1");
        payload.put("configuration.filters[0].operator", "IS_VALID");
        payload.put("configuration.filters[0].value", "VALID");
        final Object[] params = buildObjectParams("visibility", payload, MethodsHolder.FilterConfiguration.class);
        assertTrue(MethodsHolder.FilterConfiguration.class.isInstance(params[0]));
        final MethodsHolder.FilterConfiguration value = MethodsHolder.FilterConfiguration.class.cast(params[0]);
        assertEquals("VALID", value.getFilters().get(0).getValue());
    }

    @Test
    void simpleNestedRequiredActiveIfFiltersKo() throws NoSuchMethodException {
        final String expected = "- Property 'configuration.filters[${index}].value' is required.";
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.logicalOpType", "AL");
        payload.put("configuration.filters[0].columnName", "col1");
        payload.put("configuration.filters[0].operator", "IS_VALID");
        try {
            buildObjectParams("visibility", payload, MethodsHolder.FilterConfiguration.class);
            fail("Cannot reach here!");
        } catch (IllegalArgumentException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    void nestedRequiredActiveIfFiltersOk() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.logicalOpType", "AL");
        // visibility (false) should not throw Property 'configuration.filters[${index}].value' is required.
        payload.put("configuration.filters[0].columnName", "col0");
        payload.put("configuration.filters[0].operator", "IS_NULL");
        payload.put("configuration.filters[1].columnName", "col1");
        payload.put("configuration.filters[1].operator", "IS_EMPTY");
        payload.put("configuration.filters[2].columnName", "col2");
        payload.put("configuration.filters[2].operator", "IS_NOT_NULL");
        payload.put("configuration.filters[3].columnName", "col3");
        payload.put("configuration.filters[3].operator", "IS_NOT_EMPTY");
        final Object[] params = buildObjectParams("visibility", payload, MethodsHolder.FilterConfiguration.class);
        assertTrue(MethodsHolder.FilterConfiguration.class.isInstance(params[0]));
        final MethodsHolder.FilterConfiguration value = MethodsHolder.FilterConfiguration.class.cast(params[0]);
        assertEquals("AL", value.getLogicalOpType());
        assertEquals("col0", value.getFilters().get(0).getColumnName());
    }

    @Test
    void nestedRequiredActiveIfFiltersKo() throws NoSuchMethodException {
        final String expected = "- Property 'configuration.filters[${index}].value' is required.";
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.logicalOpType", "AL");
        payload.put("configuration.filters[0].columnName", "col0");
        payload.put("configuration.filters[0].operator", "IS_NULL");
        payload.put("configuration.filters[0].value", "V");
        payload.put("configuration.filters[1].columnName", "col1");
        payload.put("configuration.filters[1].operator", "IS_EMPTY");
        payload.put("configuration.filters[1].value", "V");
        // this array element SHOULD BE VISIBLE and its value required
        payload.put("configuration.filters[2].columnName", "col2");
        payload.put("configuration.filters[2].operator", "IS_VALID");
        try {
            buildObjectParams("visibility", payload, MethodsHolder.FilterConfiguration.class);
            fail("configuration.filters[2].operator=IS_VALID should be visible!");
        } catch (IllegalArgumentException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    void configWithActiveIfEnumOK() throws NoSuchMethodException {
        final Map<String, String> payload = new HashMap<String, String>();
        payload.put("configuration.bool1", "true");
        payload.put("configuration.bool2", "false");
        payload.put("configuration.bool3", "true");
        payload.put("configuration.enumRequired", "ONE");
        payload.put("configuration.enumIf", "ONE");
        payload.put("configuration.enumIfs", "ONE");
        final Object[] params = buildObjectParams("visibility", payload, MethodsHolder.ConfigWithActiveIfEnum.class);
        assertTrue(MethodsHolder.ConfigWithActiveIfEnum.class.isInstance(params[0]));
        final MethodsHolder.ConfigWithActiveIfEnum value = MethodsHolder.ConfigWithActiveIfEnum.class.cast(params[0]);
        assertTrue(value.isBool1());
    }

    @Test
    void configWithActiveIfEnumKoAll() throws NoSuchMethodException {
        final String expected =
                "- Property 'configuration.enumIf' is required.\n- Property 'configuration.enumIfs' is required.\n- Property 'configuration.enumRequired' is required.";
        final Map<String, String> payload = new HashMap();
        payload.put("configuration.bool1", "true");
        payload.put("configuration.bool2", "false");
        payload.put("configuration.bool3", "true");
        try {
            buildObjectParams("visibility", payload, MethodsHolder.ConfigWithActiveIfEnum.class);
            fail("Cannot reach here!");
        } catch (IllegalArgumentException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    private Object[] buildObjectParams(final String method, final Map<String, String> payload, final Class<?>... args)
            throws NoSuchMethodException {
        return buildObjectParams(MethodsHolder.class.getMethod(method, args), payload);
    }

    private Object[] buildObjectParams(final Method factory, final Map<String, String> payload)
            throws NoSuchMethodException {
        final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());
        final List<ParameterMeta> metas = service.buildParameterMetas(factory, "def",
                new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        return reflectionService.parameterFactory(factory, emptyMap(), metas).apply(payload);
    }

    private Function<Map<String, String>, Object[]> getComponentFactory(final Class<?> param,
            final Map<Class<?>, Object> services) throws NoSuchMethodException {
        final Constructor<FakeComponent> constructor = FakeComponent.class.getConstructor(param);
        final List<ParameterMeta> metas = parameterModelService
                .buildParameterMetas(constructor, constructor.getDeclaringClass().getPackage().getName(),
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        return reflectionService.parameterFactory(constructor, services, metas);
    }

    private Function<Map<String, String>, Object[]> getComponentFactory(final Class<?> param)
            throws NoSuchMethodException {
        return getComponentFactory(param, emptyMap());
    }

    @Data
    public static class TableOwner {

        @Option
        private List<Column> table;

        @Option
        private Map<String, Column> map;

        public static void factory(@Option("root") final TableOwner owner) {
            // no-op
        }

        public static UserHttpClient http(final UserHttpClient client) {
            return client;
        }
    }

    @Data
    public static class Column {

        @Option
        private String value1;

        @Option
        private int value2;

        @Option
        private List<Column> nestedList;
    }

    public interface UserHttpClient extends HttpClient {

        @Request
        String get();
    }

    public static class SomeConfig {

        @Option
        @Required
        private String requiredString;

        @Option
        @Min(5)
        private int integer;

        void isSet(final String rs, final int i) {
            assertEquals(requiredString, rs);
            assertEquals(integer, i);
        }
    }

    public static class SomeConfig2 {

        @Option
        private String someOtherParam;

        @Option
        @Max(1)
        private List<String> integers;
    }

    public static class SomeNestedConfig {

        @Option
        @Min(6)
        private String value;
    }

    public static class SomeConfig3 {

        @Option
        private String someOtherParam;

        @Option
        private SomeNestedConfig nested;
    }

    public static class SomeConfig4 {

        @Option
        private String someOtherParam;

        @Option
        private List<SomeNestedConfig> nesteds;
    }

    public static class SomeConfig5 {

        @Option
        @Pattern("/^[a-z]+$/")
        private String regex;
    }

    public static class SomeConfig6 {

        @Option
        @Pattern("^https?://.+\\S$")
        private String url;

        @Option
        @Pattern("^pulsar(\\+ssl)?://.*")
        private String pulsar;
    }

    public static class RequiredVisibilityPrimitive {

        @Option
        @Required
        @ActiveIf(target = "toggle", value = "true")
        private String string;

        @Option
        private boolean toggle;
    }

    public static class RequiredVisibilityArray {

        @Option
        @Required
        @ActiveIf(target = "toggle", value = "true")
        private List<String> strings;

        @Option
        private boolean toggle;
    }

    public static class RequiredList {

        @Option
        @Required
        private List<String> list;
    }

    public static class RequiredListObject {

        @Option
        @Required
        private List<SomeConfig5> list;
    }

    @EqualsAndHashCode
    public static class SomeIntegerConfig {

        @Option
        private int primitiveField;

        @Option
        private Integer objectField;

        void isSet(final Integer rs, final int i) {
            assertEquals(objectField, rs);
            assertEquals(primitiveField, i);
        }
    }

    public static class FakeComponent {

        public FakeComponent(@Option("configuration") final NestedRenamedOption config) {
            // no-op
        }

        public FakeComponent(@Option("configuration") final RenamedOption config) {
            // no-op
        }

        public FakeComponent(@Configuration("myconfig") final MyConfig config) {
            // no-op
        }

        public FakeComponent(@Option("root") final RequiredVisibilityPrimitive config) {
            // no-op
        }

        public FakeComponent(@Option("root") final RequiredVisibilityArray config) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeConfig config) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeConfig2 config2) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeConfig3 config3) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeConfig4 config4) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeConfig5 config5) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeConfig6 config6) {
            // no-op
        }

        public FakeComponent(@Option("root") final RequiredList root) {
            // no-op
        }

        public FakeComponent(@Option("root") final RequiredListObject root) {
            // no-op
        }

        public FakeComponent(@Option("root") final ConfigWithDate root) {
            // no-op
        }

        public FakeComponent(@Option("root") final JsonObject root) {
            // no-op
        }

        public FakeComponent(@Option("root") final SomeIntegerConfig root) {
            // mo-op
        }
    }

    public static class ConfigWithDate {

        @Option
        @DateTime(dateFormat = "YYYY-DD-MM", useSeconds = false, useUTC = false)
        private ZonedDateTime date;
    }

    public static class MyConfig {

        @Option
        private String url;

        @Option
        private String user;
    }

    public static class RenamedOption {

        @Option("$url")
        private String url;

        @Override
        public String toString() {
            return url;
        }
    }

    public static class NestedRenamedOption {

        @Option("$option")
        private RenamedOption option;

        @Override
        public String toString() {
            return option.toString();
        }
    }

    @Test
    void createObjectFactoryDataset() {
        final Map conf = new HashMap<String, String>() {

            {
                put("connection.type", "ibmdb2");
                put("connection.url", "jdbc://hurle");
                put("connection.active", "true");
                put("connection.state", "PENDING");
                put("tableName", "tableWithName");
                put("maxCount", "30");
                put("table", "test1"); // non existent member
                put("listColumns[0]", "col0");
                put("listColumns[1]", "col1");
                put("listColumns[2]", "col2");
                put("nestedConfigs[0].value", "value0");
            }
        };
        final TableNameDataset dataset = reflectionService.createObjectFactory(TableNameDataset.class).apply("", conf);
        assertEquals("ibmdb2", dataset.getConnection().getType());
        assertEquals("jdbc://hurle", dataset.getConnection().getUrl());
        assertTrue(dataset.getConnection().isActive());
        assertEquals(State.PENDING, dataset.getConnection().getState());
        assertEquals(30, dataset.getMaxCount());
        assertEquals("tableWithName", dataset.getTableName());
        assertEquals(3, dataset.getListColumns().size());
        assertEquals("value0", dataset.getNestedConfigs().get(0).value);
        assertNull(dataset.getNullConfigs());
        // test with prefix name
        final JdbcConnection connection =
                reflectionService.createObjectFactory(JdbcConnection.class).apply("connection", conf);
        assertEquals("ibmdb2", dataset.getConnection().getType());
        assertEquals("jdbc://hurle", dataset.getConnection().getUrl());
        assertTrue(dataset.getConnection().isActive());
        assertEquals(State.PENDING, connection.getState());
    }

    @Test
    void createObjectFactoryPrimitives() {
        final Map conf = new HashMap<String, String>() {

            {
                put("state", "PENDING");
                put("double", "12345.6789");
                put("boolean", "true");
            }
        };
        assertEquals("PENDING", reflectionService.createObjectFactory(String.class).apply("state", conf));
        assertEquals(12345.6789, reflectionService.createObjectFactory(Double.class).apply("double", conf));
        assertEquals(12345.6789, reflectionService.createObjectFactory(double.class).apply("double", conf));
        assertTrue(reflectionService.createObjectFactory(Boolean.class).apply("boolean", conf));
        assertTrue(reflectionService.createObjectFactory(boolean.class).apply("boolean", conf));
    }

    @Test
    void createObjectFactoryEnum() {
        final Map conf = new HashMap<String, String>() {

            {
                put("state", "PENDING");
                put("closed", "CLOSED");
                put("open", "OPEN");
            }
        };
        assertEquals(reflectionService.createObjectFactory(State.class).apply("state", conf), State.PENDING);
        assertEquals(reflectionService.createObjectFactory(State.class).apply("closed", conf), State.CLOSED);
        assertEquals(reflectionService.createObjectFactory(State.class).apply("open", conf), State.OPEN);
    }

    public enum State {
        OPEN,
        PENDING,
        CLOSED
    }

    @Data
    @DataStore("JdbcConnection")
    public static class JdbcConnection implements Serializable {

        @Option
        private String type;

        @Option
        private String url;

        @Option
        private boolean active;

        @Option
        private State state;
    }

    @Data
    @DataSet("TableNameDataset")
    public static class TableNameDataset implements Serializable {

        @Option
        private JdbcConnection connection;

        @Option
        private String tableName;

        @Option
        private int maxCount;

        @Option
        private List<String> listColumns;

        @Option
        private List<SomeNestedConfig> nestedConfigs;

        @Option
        private List<SomeNestedConfig> nullConfigs;
    }
}
