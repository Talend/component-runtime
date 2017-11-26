/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.test.MethodsHolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParameterModelServiceTest {

    private final ParameterModelService service = new ParameterModelService();

    @Test
    public void primitive() throws NoSuchMethodException {
        final List<ParameterMeta> params = service.buildParameterMetas(
                MethodsHolder.class.getMethod("primitives", String.class, String.class, int.class), "def");
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
            assertEquals("arg1", param.getName());
            assertEquals("arg1", param.getPath());
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
    public void collection() throws NoSuchMethodException {
        final List<ParameterMeta> params = service.buildParameterMetas(
                MethodsHolder.class.getMethod("collections", List.class, List.class, Map.class), "def");
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
    public void array() throws NoSuchMethodException {
        final List<ParameterMeta> params =
                service.buildParameterMetas(MethodsHolder.class.getMethod("array", MethodsHolder.Array.class), "def");
        assertEquals(1, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("arg0", param.getName());
            assertEquals("arg0", param.getPath());
            assertEquals(ParameterMeta.Type.OBJECT, param.getType());
            assertEquals(1, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());
            assertTrue(param.getMetadata().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("urls", nested.getName());
                assertEquals("arg0.urls", nested.getPath());
                assertEquals(ParameterMeta.Type.ARRAY, nested.getType());
                assertEquals(1, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());

                {
                    final ParameterMeta nestedItem = nested.getNestedParameters().get(0);
                    assertEquals("urls[${index}]", nestedItem.getName());
                    assertEquals("arg0.urls[${index}]", nestedItem.getPath());
                    assertEquals(ParameterMeta.Type.STRING, nestedItem.getType());
                    assertTrue(nestedItem.getNestedParameters().isEmpty());
                    assertTrue(nestedItem.getProposals().isEmpty());
                }
            }
        }
    }

    @Test
    public void object() throws NoSuchMethodException {
        HashMap<String, String> expectedDataSet = new HashMap<String, String>() {

            {
                put("tcomp::configurationtype::type", "dataset");
                put("tcomp::configurationtype::name", "test");
            }
        };
        final List<ParameterMeta> params = service.buildParameterMetas(
                MethodsHolder.class.getMethod("object", MethodsHolder.Config.class, MethodsHolder.Config.class), "def");
        assertEquals(2, params.size());
        assertConfigModel("arg0", params.get(0));
        assertConfigModel("prefixed", params.get(1));
        assertEquals(expectedDataSet, params.get(0).getMetadata());
        assertEquals(expectedDataSet, params.get(1).getMetadata());
    }

    @Test
    public void nestedObject() throws NoSuchMethodException {
        final List<ParameterMeta> params = service.buildParameterMetas(
                MethodsHolder.class.getMethod("nested", MethodsHolder.ConfigOfConfig.class), "def");
        assertEquals(1, params.size());
        {
            final ParameterMeta param = params.get(0);
            assertEquals("arg0", param.getName());
            assertEquals("arg0", param.getPath());
            assertEquals(ParameterMeta.Type.OBJECT, param.getType());
            assertEquals(4, param.getNestedParameters().size());
            assertTrue(param.getProposals().isEmpty());

            {
                final ParameterMeta nested = param.getNestedParameters().get(0);
                assertEquals("direct", nested.getName());
                assertEquals("arg0.direct", nested.getPath());
                assertEquals(ParameterMeta.Type.OBJECT, nested.getType());
                assertEquals(2, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());
                assertConfigFieldsModel("arg0.direct", nested.getNestedParameters().get(0),
                        nested.getNestedParameters().get(1));
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(1);
                assertEquals("keyed", nested.getName());
                assertEquals("arg0.keyed", nested.getPath());
                assertEquals(ParameterMeta.Type.OBJECT, nested.getType());
                assertEquals(3, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());

                {
                    final ParameterMeta key = nested.getNestedParameters().get(0);
                    assertEquals("keyed.key[${index}]", key.getName());
                    assertEquals("arg0.keyed.key[${index}]", key.getPath());
                    assertEquals(ParameterMeta.Type.STRING, key.getType());
                    assertTrue(key.getNestedParameters().isEmpty());
                    assertTrue(key.getProposals().isEmpty());
                }
                assertConfigFieldsModel("arg0.keyed.value[${index}]", nested.getNestedParameters().get(1),
                        nested.getNestedParameters().get(2));
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(2);
                assertEquals("multiple", nested.getName());
                assertEquals("arg0.multiple", nested.getPath());
                assertEquals(ParameterMeta.Type.ARRAY, nested.getType());
                assertEquals(2, nested.getNestedParameters().size());
                assertTrue(nested.getProposals().isEmpty());
                assertConfigFieldsModel("arg0.multiple[${index}]", nested.getNestedParameters().get(1),
                        nested.getNestedParameters().get(0));
            }
            {
                final ParameterMeta nested = param.getNestedParameters().get(3);
                assertEquals("passthrough", nested.getName());
                assertEquals("arg0.passthrough", nested.getPath());
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
        assertEquals(param.getNestedParameters().toString(), 2, param.getNestedParameters().size());
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
}
