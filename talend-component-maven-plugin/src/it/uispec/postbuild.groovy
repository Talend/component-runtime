/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import java.nio.file.Paths
import java.util.zip.ZipFile

/**
 * Checks that the uispec mojo generated its archive even if the plugin classpath is polluted by conflicting libraries
 */
static def checkUiSpecMojo(final File basedir) {
    final File zip = Paths.get(basedir.toString()).resolve('target/talend-component-kit/uispec.zip').toFile()
    if (!zip.exists()) {
        throw new FileNotFoundException("Could not find uispec archive ${zip}")
    }
    final ZipFile archive = new ZipFile(zip)
    try {
        final List<String> names = archive.entries().collect { it.name }
        assert names.contains('component/')
        assert names.contains('configuration/')
        assert names.any { it.startsWith('component/') && it != 'component/' }
    } finally {
        archive.close()
    }
}

checkUiSpecMojo(basedir)
