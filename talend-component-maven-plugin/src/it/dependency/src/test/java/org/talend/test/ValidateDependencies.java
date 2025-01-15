/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.test;

import static java.util.stream.Collectors.joining;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

public class ValidateDependencies {
    @Test
    public void assertDependencies() throws IOException {
        final File dependenciesTxt = new File(jarLocation(ValidateDependencies.class), "TALEND-INF/dependencies.txt");
        assertTrue(dependenciesTxt.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(dependenciesTxt))) {
            assertEquals("", reader.lines().collect(joining("\n")));
        }
    }
}