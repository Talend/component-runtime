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
package org.talend.sdk.component.runtime.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.configuration.Configuration;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalCacheService;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.service.configuration.PropertiesConfiguration;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.manager.test.MethodsHolder;

import lombok.Data;

class ReflectionServiceTest {

    private final PropertyEditorRegistry registry = new PropertyEditorRegistry() {

        {
            register(new AbstractConverter(Integer.class) {

                @Override
                protected Object toObjectImpl(final String text) {
                    return Double.valueOf(text).intValue();
                }
            });
        }
    };

    private final ParameterModelService parameterModelService = new ParameterModelService(registry);

    private final ReflectionService reflectionService = new ReflectionService(parameterModelService, registry);

    @Test
    void configurationFromLocalConf() throws NoSuchMethodException {
        final Properties properties = new Properties();
        properties.setProperty("test.myconfig.url", "http://foo");
        properties.setProperty("myconfig.user", "set");
        final Function<Map<String, String>, Object[]> factory =
                getComponentFactory(MyConfig.class, new HashMap<Class<?>, Object>() {

                    {
                        put(LocalConfiguration.class, new LocalConfigurationService(
                                singletonList(new PropertiesConfiguration(properties)), "test"));
                        put(LocalCache.class, new LocalCacheService("test"));
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
        assertThrows(IllegalArgumentException.class, () -> factory.apply(new HashMap<String, String>() {

            {
                put("root.toggle", "true");
            }
        }));
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

    public static class FakeComponent {

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

        public FakeComponent(@Option("root") final RequiredList root) {
            // no-op
        }

        public FakeComponent(@Option("root") final RequiredListObject root) {
            // no-op
        }
    }

    public static class MyConfig {

        @Option
        private String url;

        @Option
        private String user;
    }
}
