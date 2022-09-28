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
package org.talend.sdk.component.runtime.manager;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.test.MethodsHolder;
import org.talend.sdk.component.runtime.manager.xbean.converter.ZonedDateTimeConverter;

class ParameterModelServiceTest {

    private final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry() {

        {
            register(new ZonedDateTimeConverter());
        }
    });

    @Test
    void implicitAnnotations() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("date", ZonedDateTime.class), "def", context());
        assertEquals(1, params.size());

        final ParameterMeta date = params.iterator().next();
        assertEquals(ParameterMeta.Type.STRING, date.getType());
        assertEquals(singletonMap("tcomp::ui::datetime", "zoneddatetime"), date.getMetadata());
    }

    @Test
    void primitive() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("primitives", String.class, String.class, int.class),
                        "def", context());
        assertEquals(3, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("url", param.getName());
            assertEquals("url", param.getPath());
            assertEquals(ParameterMeta.Type.STRING, param.getType());
            assertTrue(param.getNestedParameters().isEmpty());
            assertTrue(param.getProposals().isEmpty());
        }
        {
            final ParameterMeta param = params.get(1);
            assertEquals("defaultName", param.getName());
            assertEquals("defaultName", param.getPath());
            assertEquals(ParameterMeta.Type.STRING, param.getType());
            assertTrue(param.getNestedParameters().isEmpty());
            assertTrue(param.getProposals().isEmpty());
        }
        {
            final ParameterMeta param = params.get(2);
            assertEquals("port", param.getName());
            assertEquals("port", param.getPath());
            assertEquals(ParameterMeta.Type.NUMBER, param.getType());
            assertTrue(param.getNestedParameters().isEmpty());
            assertTrue(param.getProposals().isEmpty());
        }
    }

    @Test
    void charAsString() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("charOption", char.class, Character.class), "def",
                        context());
        assertEquals(2, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("delimiter", param.getPath());
            assertEquals(ParameterMeta.Type.STRING, param.getType());
            assertEquals("1", param.getMetadata().get("tcomp::validation::minLength"),
                    () -> param.getMetadata().toString());
            assertEquals("1", param.getMetadata().get("tcomp::validation::maxLength"),
                    () -> param.getMetadata().toString());
        }
        {
            final ParameterMeta param = params.get(1);
            assertEquals("delimiter2", param.getPath());
            assertEquals(ParameterMeta.Type.STRING, param.getType());
            assertEquals("1", param.getMetadata().get("tcomp::validation::maxLength"),
                    () -> param.getMetadata().toString());
            assertNull(param.getMetadata().get("tcomp::validation::minLength"), () -> param.getMetadata().toString());
        }
    }

    @Test
    void intWithDefaultBoundaries() throws NoSuchMethodException {
        final List<ParameterMeta> params = service.buildParameterMetas(
                MethodsHolder.class.getMethod("intOption", int.class, Integer.class), "def", context());
        assertEquals(2, params.size());

        final List<String> expectedPaths = Arrays.asList("foo1", "foo2");
        for (int i = 0; i < params.size(); i++) {
            final String expectedPath = expectedPaths.get(i);
            final ParameterMeta param = params.get(i);

            assertEquals(expectedPath, param.getPath());
            assertEquals(ParameterMeta.Type.NUMBER, param.getType());
            assertEquals(String.valueOf((double) Integer.MIN_VALUE), param.getMetadata().get("tcomp::validation::min"),
                    () -> param.getMetadata().toString());
            assertEquals(String.valueOf((double) Integer.MAX_VALUE), param.getMetadata().get("tcomp::validation::max"),
                    () -> param.getMetadata().toString());
        }
    }

    @Test
    void intWithOverwrittenDefaultBoundaries() throws NoSuchMethodException {
        final List<ParameterMeta> params = service.buildParameterMetas(
                MethodsHolder.class.getMethod("intOptionOverwrite", int.class, Integer.class), "def", context());
        assertEquals(2, params.size());

        {
            final ParameterMeta param = params.get(0);

            assertEquals("foo1", param.getPath());
            assertEquals(ParameterMeta.Type.NUMBER, param.getType());
            assertEquals("42.0", param.getMetadata().get("tcomp::validation::min"),
                    () -> param.getMetadata().toString());
            assertEquals(String.valueOf((double) Integer.MAX_VALUE), param.getMetadata().get("tcomp::validation::max"),
                    () -> param.getMetadata().toString());
        }
        {
            final ParameterMeta param = params.get(1);

            assertEquals("foo2", param.getPath());
            assertEquals(ParameterMeta.Type.NUMBER, param.getType());
            assertEquals(String.valueOf((double) Integer.MIN_VALUE), param.getMetadata().get("tcomp::validation::min"),
                    () -> param.getMetadata().toString());
            assertEquals("42.0", param.getMetadata().get("tcomp::validation::max"),
                    () -> param.getMetadata().toString());
        }
    }

    @Test
    void collection() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("collections", List.class, List.class, Map.class),
                        "def", context());
        assertEquals(3, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("urls", param.getName());
            assertEquals("urls", param.getPath());
            assertEquals(ParameterMeta.Type.ARRAY, param.getType());
            assertEquals(1, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("urls[${index}]", nested.getName());
                assertEquals("urls[${index}]", nested.getPath());
                assertEquals(ParameterMeta.Type.STRING, nested.getType());
                assertTrue(nested.getNestedParameters().isEmpty());
                assertTrue(nested.getProposals().isEmpty());
            }
        }
        {
            final ParameterMeta param = params.get(1);
            assertEquals("ports", param.getName());
            assertEquals("ports", param.getPath());
            assertEquals(ParameterMeta.Type.ARRAY, param.getType());
            assertEquals(1, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("ports[${index}]", nested.getName());
                assertEquals("ports[${index}]", nested.getPath());
                assertEquals(ParameterMeta.Type.NUMBER, nested.getType());
                assertTrue(nested.getNestedParameters().isEmpty());
                assertTrue(nested.getProposals().isEmpty());
            }
        }
        {
            final ParameterMeta param = params.get(2);
            assertEquals("mapping", param.getName());
            assertEquals("mapping", param.getPath());
            assertEquals(ParameterMeta.Type.OBJECT, param.getType());
            assertEquals(2, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("mapping.key[${index}]", nested.getName());
                assertEquals("mapping.key[${index}]", nested.getPath());
                assertEquals(ParameterMeta.Type.STRING, nested.getType());
                assertTrue(nested.getNestedParameters().isEmpty());
                assertTrue(nested.getProposals().isEmpty());
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(1);
                assertEquals("mapping.value[${index}]", nested.getName());
                assertEquals("mapping.value[${index}]", nested.getPath());
                assertEquals(ParameterMeta.Type.STRING, nested.getType());
                assertTrue(nested.getNestedParameters().isEmpty());
                assertTrue(nested.getProposals().isEmpty());
            }
        }
    }

    @Test
    void array() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("array", MethodsHolder.Array.class), "def",
                        context());
        assertEquals(1, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("value", param.getName());
            assertEquals("value", param.getPath());
            assertEquals(ParameterMeta.Type.OBJECT, param.getType());
            assertEquals(1, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());
            assertTrue(param.getMetadata().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("urls", nested.getName());
                assertEquals("value.urls", nested.getPath());
                assertEquals(ParameterMeta.Type.ARRAY, nested.getType());
                assertEquals(1, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());

                {
                    final ParameterMeta nestedItem = nested.getNestedParameters().get(0);
                    assertEquals("urls[${index}]", nestedItem.getName());
                    assertEquals("value.urls[${index}]", nestedItem.getPath());
                    assertEquals(ParameterMeta.Type.STRING, nestedItem.getType());
                    assertTrue(nestedItem.getNestedParameters().isEmpty());
                    assertTrue(nestedItem.getProposals().isEmpty());
                }
            }
        }
    }

    @Test
    void object() throws NoSuchMethodException {
        HashMap<String, String> expectedDataSet = new HashMap<String, String>() {

            {
                put("tcomp::configurationtype::type", "dataset");
                put("tcomp::configurationtype::name", "test");
            }
        };
        final List<ParameterMeta> params = service
                .buildParameterMetas(
                        MethodsHolder.class.getMethod("object", MethodsHolder.Config.class, MethodsHolder.Config.class),
                        "def", context());
        assertEquals(2, params.size());
        assertConfigModel("implicit", params.get(0));
        assertConfigModel("prefixed", params.get(1));
        assertEquals(expectedDataSet, params.get(0).getMetadata());
        assertEquals(expectedDataSet, params.get(1).getMetadata());

        assertEquals("test",
                params.get(0).getNestedParameters().get(1).getMetadata().get("tcomp::action::dynamic_values"));
        assertNull(params
                .get(0)
                .getNestedParameters()
                .get(1)
                .getNestedParameters()
                .get(0)
                .getMetadata()
                .get("tcomp::action::dynamic_values"));
    }

    @Test
    void nestedObject() throws NoSuchMethodException {
        final List<ParameterMeta> params = service
                .buildParameterMetas(MethodsHolder.class.getMethod("nested", MethodsHolder.ConfigOfConfig.class), "def",
                        context());
        assertEquals(1, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("value", param.getName());
            assertEquals("value", param.getPath());
            assertEquals(ParameterMeta.Type.OBJECT, param.getType());
            assertEquals(4, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("direct", nested.getName());
                assertEquals("value.direct", nested.getPath());
                assertEquals(ParameterMeta.Type.OBJECT, nested.getType());
                assertEquals(2, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());
                assertConfigFieldsModel("value.direct", nested.getNestedParameters().get(0),
                        nested.getNestedParameters().get(1));
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(1);
                assertEquals("keyed", nested.getName());
                assertEquals("value.keyed", nested.getPath());
                assertEquals(ParameterMeta.Type.OBJECT, nested.getType());
                assertEquals(3, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());

                {
                    final ParameterMeta key = nested.getNestedParameters().get(0);
                    assertEquals("keyed.key[${index}]", key.getName());
                    assertEquals("value.keyed.key[${index}]", key.getPath());
                    assertEquals(ParameterMeta.Type.STRING, key.getType());
                    assertTrue(key.getNestedParameters().isEmpty());
                    assertTrue(key.getProposals().isEmpty());
                }
                assertConfigFieldsModel("value.keyed.value[${index}]", nested.getNestedParameters().get(1),
                        nested.getNestedParameters().get(2));
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(2);
                assertEquals("multiple", nested.getName());
                assertEquals("value.multiple", nested.getPath());
                assertEquals(ParameterMeta.Type.ARRAY, nested.getType());
                assertEquals(2, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());
                assertConfigFieldsModel("value.multiple[${index}]", nested.getNestedParameters().get(1),
                        nested.getNestedParameters().get(0));
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(3);
                assertEquals("passthrough", nested.getName());
                assertEquals("value.passthrough", nested.getPath());
                assertEquals(ParameterMeta.Type.STRING, nested.getType());
                assertTrue(nested.getNestedParameters().isEmpty());
                assertTrue(nested.getProposals().isEmpty());
            }
        }
    }

    private void assertConfigModel(final String prefix, final ParameterMeta param) {
        assertEquals(prefix, param.getName().substring(prefix.lastIndexOf('.') + 1));
        assertEquals(prefix, param.getPath());
        assertEquals(ParameterMeta.Type.OBJECT, param.getType());
        assertEquals(2, param.getNestedParameters().size(), param.getNestedParameters().toString());
        assertTrue(param.getProposals().isEmpty());

        assertConfigFieldsModel(prefix, param.getNestedParameters().get(0), param.getNestedParameters().get(1));
    }

    private void assertConfigFieldsModel(final String prefix, final ParameterMeta mapping, final ParameterMeta url) {
        {
            assertEquals("mapping", mapping.getName());
            assertEquals(prefix + ".mapping", mapping.getPath());
            assertEquals(ParameterMeta.Type.OBJECT, mapping.getType());
            assertEquals(2, mapping.getNestedParameters().size());
            assertTrue(mapping.getProposals().isEmpty());

            {
                final ParameterMeta nestedItem = mapping.getNestedParameters().get(0);
                assertEquals("mapping.key[${index}]", nestedItem.getName());
                assertEquals(prefix + ".mapping.key[${index}]", nestedItem.getPath());
                assertEquals(ParameterMeta.Type.STRING, nestedItem.getType());
                assertTrue(nestedItem.getNestedParameters().isEmpty());
                assertTrue(nestedItem.getProposals().isEmpty());
            }
            {
                final ParameterMeta nestedItem = mapping.getNestedParameters().get(1);
                assertEquals("mapping.value[${index}]", nestedItem.getName());
                assertEquals(prefix + ".mapping.value[${index}]", nestedItem.getPath());
                assertEquals(ParameterMeta.Type.STRING, nestedItem.getType());
                assertTrue(nestedItem.getNestedParameters().isEmpty());
                assertTrue(nestedItem.getProposals().isEmpty());
            }
        }
        {
            assertEquals("urls", url.getName());
            assertEquals(prefix + ".urls", url.getPath());
            assertEquals(ParameterMeta.Type.ARRAY, url.getType());
            assertEquals(1, url.getNestedParameters().size());
            assertTrue(url.getProposals().isEmpty());

            {
                final ParameterMeta nestedItem = url.getNestedParameters().get(0);
                assertEquals("urls[${index}]", nestedItem.getName());
                assertEquals(prefix + ".urls[${index}]", nestedItem.getPath());
                assertEquals(ParameterMeta.Type.STRING, nestedItem.getType());
                assertTrue(nestedItem.getNestedParameters().isEmpty());
                assertTrue(nestedItem.getProposals().isEmpty());
            }
        }
    }

    private BaseParameterEnricher.Context context() {
        return new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test"));
    }
}
