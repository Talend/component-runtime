// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.container.maven.shade;

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

        final Properties properties = artifacts.stream().collect(Properties::new, (props, artifact) -> {
            final String filename = String.format("%s-%s%s.%s", artifact.getArtifactId(), artifact.getVersion(),
                    ofNullable(artifact.getClassifier()).filter(c -> !c.isEmpty()).map(c -> '-' + c).orElse(""),
                    ofNullable(artifact.getType()).orElse("jar"));
            final String pluginName = artifact.getArtifactId()
                    + (ofNullable(artifact.getClassifier()).filter(c -> !c.isEmpty()).map(c -> '-' + c).orElse(""));
            props.setProperty(pluginName, String.format("%s/%s/%s/%s", artifact.getGroupId().replace(".", "/"),
                    artifact.getArtifactId(), artifact.getVersion(), filename));
        }, Hashtable::putAll);
        jarOutputStream.putNextEntry(new ZipEntry(pluginListResource));
        properties.store(jarOutputStream, "plugin list");
    }
}
