/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import lombok.Data;

import java.io.File;
import java.util.Map;

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

    private String apiVersion = "${component-api.version}";

    //
    // deploy in studio
    //
    private File studioHome;

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
}
