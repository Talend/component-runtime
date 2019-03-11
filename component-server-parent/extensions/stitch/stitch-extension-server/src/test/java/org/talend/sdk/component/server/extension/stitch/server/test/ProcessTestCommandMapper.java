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

import static java.util.Arrays.asList;

import java.nio.file.Paths;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;

import org.talend.sdk.component.server.extension.stitch.server.execution.ProcessCommandMapper;

@Specializes
@ApplicationScoped
public class ProcessTestCommandMapper extends ProcessCommandMapper {

    @Override
    public List<String> toCommand(final String tap, final String configPath) {
        switch (tap) {
        case "ProcessExecutorTest#execute":
            return asList(java(), "-cp", "target/test-classes", MainTestImpl.class.getName());
        default:
            return super.toCommand(tap, configPath);
        }
    }

    private String java() {
        return Paths.get(System.getProperty("java.home")).resolve("bin/java").toAbsolutePath().toString();
    }
}
