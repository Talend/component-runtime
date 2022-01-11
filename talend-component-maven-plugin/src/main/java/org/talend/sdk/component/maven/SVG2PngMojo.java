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
package org.talend.sdk.component.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.talend.sdk.component.tools.SVG2Png;

@Mojo(name = "svg2png", defaultPhase = PROCESS_CLASSES, threadSafe = true)
public class SVG2PngMojo extends AbstractMojo {

    @Parameter(defaultValue = "false", property = "talend.skip")
    private boolean skip;

    /**
     * Some SVG can use alpha to draw the shape and not colors, here we force the shape to appear in PNG
     * because Talend Studio - eclipse actually - caches using RGC channels and not alpha one.
     */
    @Parameter(defaultValue = "true", property = "talend.icons.workaround")
    private boolean workarounds;

    /**
     * Folder to process.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/icons", property = "talend.icons.source")
    protected File icons;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (icons == null || !icons.exists()) {
            getLog().debug("No icons folder, skipping");
            return;
        }

        new SVG2Png(icons.toPath(), workarounds, getLog()).run();
    }
}
