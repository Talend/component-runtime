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
package org.talend.sdk.component.server.extension.stitch.server.test;

import static java.util.Collections.emptyMap;

import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.apache.meecrowave.Meecrowave;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class ConfigTestSource implements ConfigSource {

    @Override
    public Map<String, String> getProperties() {
        return emptyMap();
    }

    @Override
    public String getValue(final String propertyName) {
        switch (propertyName) {
        case "talend.stitch.service.command.mapping":
            return "ProcessCommandMapperTest_mapped = foo bar\nProcessCommandMapperTest_placeholder = ${configurationFile} ${tap} ${replaced.by.config}";
        case "replaced.by.config":
            return "done";
        case "talend.server.extension.stitch.client.base":
            return "http://localhost:" + CDI.current().select(Meecrowave.Builder.class).get().getHttpPort()
                    + "/api/v1/stitch";
        case "talend.server.extension.stitch.token":
            return "test-token";
        case "talend.server.extension.stitch.client.retries":
            return "5";
        default:
            return null;
        }
    }

    @Override
    public String getName() {
        return "test";
    }
}
