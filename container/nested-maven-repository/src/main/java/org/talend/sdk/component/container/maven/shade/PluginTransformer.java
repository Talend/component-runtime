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
package org.talend.sdk.component.container.maven.shade;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import lombok.Setter;

@Setter
public class PluginTransformer extends ArtifactTransformer {

    @Setter
    private String pluginListResource = "TALEND-INF/plugins.properties";

    @Override
    public void modifyOutputStream(final JarOutputStream jarOutputStream) throws IOException {
        if (artifacts.isEmpty()) {
            return;
        }

        final Properties properties =
                artifacts.stream().filter(this::isComponent).collect(Properties::new, (props, artifact) -> {
                    final String filename = String
                            .format("%s-%s%s.%s", artifact.getArtifactId(), artifact.getVersion(),
                                    ofNullable(artifact.getClassifier())
                                            .filter(c -> !c.isEmpty())
                                            .map(c -> '-' + c)
                                            .orElse(""),
                                    ofNullable(artifact.getType()).orElse("jar"));
                    final String pluginName = artifact.getArtifactId() + (ofNullable(artifact.getClassifier())
                            .filter(c -> !c.isEmpty())
                            .map(c -> '-' + c)
                            .orElse(""));
                    props
                            .setProperty(pluginName,
                                    String
                                            .format("%s/%s/%s/%s", artifact.getGroupId().replace(".", "/"),
                                                    artifact.getArtifactId(), artifact.getVersion(), filename));
                }, Hashtable::putAll);
        jarOutputStream.putNextEntry(new ZipEntry(pluginListResource));
        properties.store(jarOutputStream, "plugin list");
    }
}
