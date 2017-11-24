/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.junit.Test;

public class ResolverImplTest {

    @Test
    public void resolvefromDescriptor() throws IOException {
        try (final InputStream stream = new ByteArrayInputStream(
            "The following files have been resolved:\njunit:junit:jar:4.12:compile".getBytes(StandardCharsets.UTF_8))) {
            final Collection<File> deps =
                new ResolverImpl(null, coord -> new File("maven2", coord)).resolveFromDescriptor(stream);
            assertEquals(1, deps.size());
            String sep = File.separator;
            StringBuilder expectedPath = new StringBuilder().append("maven2").append(sep).append("junit").append(sep)
                .append("junit").append(sep).append("4.12").append(sep).append("junit-4.12.jar");
            assertEquals(expectedPath.toString(), deps.iterator().next().getPath());
        }
    }
}
