/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.http;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.http.Base;
import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.ConfigurerOption;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.QueryParams;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.UseConfigurer;
import org.talend.sdk.component.runtime.manager.service.http.RequestParser.InstanceCreator;
import org.talend.sdk.component.runtime.manager.service.http.codec.JsonpDecoder;

class RequestParserTest {

    @Request(path = "/test/basic")
    String hello() {
        return "Hello";
    }

    @UseConfigurer(MyConfigurer.class)
    @Request(path = "/{path1}/p2", method = "POST")
    Integer complexe(@ConfigurerOption("connection") String connection, //
            @Header("AUTHORIZATION") String auth, //
            @Base String newBase, @Path("path1") String path1, @Query("timeout") Integer timeout,
            @QueryParams(encode = false) Map<String, String> qp1) {
        return 3;
    }

    @Test
    void parse() throws NoSuchMethodException {
        final Jsonb jsonb = JsonbBuilder.create();
        final InstanceCreator instanceCreator = new InstanceCreator() {

            @Override
            public <T> T buildNew(Class<? extends T> realClass) {
                if (Encoder.class.isAssignableFrom(realClass)) {
                    return (T) new JsonpDecoder(jsonb);
                }
                if (Decoder.class.isAssignableFrom(realClass)) {
                    return (T) new JsonpDecoder(jsonb);
                }
                return null;
            }
        };
        final RequestParser parser = new RequestParser(instanceCreator, jsonb);
        {
            final ExecutionContext hello = parser.parse(RequestParserTest.class.getDeclaredMethod("hello"));
            final Type responseType = hello.getResponseType();
            Assertions.assertEquals(String.class, responseType);
            final HttpRequestCreator helloCreator = hello.getRequestCreator();
            Assertions.assertNotNull(helloCreator);
            final HttpRequest request = helloCreator.apply("http://base", new String[] {});
            Assertions.assertNotNull(request);

            Assertions.assertTrue(request.getHeaders().isEmpty());
            Assertions.assertEquals("http://base/test/basic", request.getUrl());
            Assertions.assertEquals("GET", request.getMethodType());

            Assertions.assertTrue(request.getQueryParams().isEmpty());
        }

        final ExecutionContext complexe = parser
                .parse(RequestParserTest.class
                        .getDeclaredMethod("complexe", String.class, String.class, String.class, String.class,
                                Integer.class, Map.class));
        {
            final Type responseType = complexe.getResponseType();
            final HttpRequestCreator creator = complexe.getRequestCreator();
            Assertions.assertNotNull(creator);
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("arg1", "value1");
            queryParams.put("arg2", "value2");
            final HttpRequest request = creator
                    .apply("http://base", new Object[] { "MyCnx", "AuthentString", "https://custombase/", "mypath1",
                            2355, queryParams });
            Assertions.assertNotNull(request);
            Assertions.assertEquals("https://custombase/mypath1/p2", request.getUrl());
            final Collection<String> requestParams = request.getQueryParams();
            Assertions.assertEquals(3, requestParams.size());
            Assertions.assertTrue(requestParams.contains("timeout=2355"));
            Assertions.assertTrue(requestParams.contains("arg1=value1"));
        }

    }

    public static class MyConfigurer implements Configurer {

        @Override
        public void configure(Connection connection, ConfigurerConfiguration configuration) {
        }
    }
}