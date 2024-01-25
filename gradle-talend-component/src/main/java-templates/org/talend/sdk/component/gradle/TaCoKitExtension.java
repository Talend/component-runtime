/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.gradle;

import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.ROOT;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import lombok.Data;

@Data
public class TaCoKitExtension {

    //
    // dependency task
    //

    private String dependenciesLocation = "TALEND-INF/dependencies.txt";

    private boolean skipDependenciesFile = false;

    //
    // classpath for validation utilities
    //

    private String sdkVersion = "${project.version}";

    private String apiVersion = "${project.version}";

    private String meecrowaveVersion = "${meecrowave.version}";

    private String commonsCliVersion = "${commons-cli.version}";

    //
    // deploy in studio
    //
    private File studioHome;

    private File studioM2;

    private boolean enforceDeployment = false;

    //
    // documentation
    //
    private boolean skipDocumentation = false;

    private int documentationLevel = 2;

    private File documentationOutput;

    private Map<String, String> documentationFormats;

    private Map<String, String> documentationAttributes;

    private File documentationTemplateDir;

    private String documentationTitle;

    private String documentationTemplateEngine;

    private Collection<Locale> documentationLocales = asList(ROOT, ENGLISH);

    //
    // validation
    //
    private boolean skipValidation = false;

    private boolean validateFamily = true;

    private boolean validateSerializable = true;

    private boolean validateInternationalization = true;

    private boolean validateModel = true;

    private boolean validateMetadata = true;

    private boolean validateComponent = true;

    private boolean validateDataStore = true;

    private boolean validateDataSet = true;

    private boolean validateActions = true;

    private boolean validateDocumentation = true;

    private boolean validateLayout = true;

    private boolean validateOptionNames = true;

    private boolean validateLocalConfiguration = true;

    private boolean validateOutputConnection = true;

    private boolean validatePlaceholder = true;

    private boolean validateSvg = true;

    private boolean validateNoFinalOption = true;

    private String pluginId;

    //
    // web
    //
    private Collection<String> serverArguments;

    private Integer serverPort;

    private boolean openInBrowser = true;

    //
    // car bundling
    //
    private File carOutput;

    private Map<String, String> carMetadata;

    //
    // svg2png
    //
    private File icons;

    private boolean useIconWorkarounds;
}
