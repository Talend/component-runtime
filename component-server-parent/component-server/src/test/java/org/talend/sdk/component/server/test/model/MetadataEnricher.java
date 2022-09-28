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
package org.talend.sdk.component.server.test.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.spi.component.ComponentMetadataEnricher;

public class MetadataEnricher implements ComponentMetadataEnricher {

    @Override
    public Map<String, String> onComponent(final Type type, final Annotation[] annotations) {
        return new HashMap<String, String>() {

            {
                put("testing::type", type.getTypeName());
                put("testing::v2", "2");
                put("testing::v3", "3");
                put("testing::v4", "4");
                put("testing::v5", "5");
            }
        };
    }

    @Override
    public int order() {
        return 100;
    }
}
